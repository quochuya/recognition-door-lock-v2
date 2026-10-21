package com.example.lock.access.rule.impl;

import com.example.lock.access.entity.AccessMethod;
import com.example.lock.access.result.RuleDecision;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.access.rule.AccessRule;
import com.example.lock.access.service.QrTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Tương đương CardStatusRule nhưng cho phương thức QR — kiểm tra vé QR còn
 * hạn, chưa dùng, và ĐÚNG PHÒNG. SKIP ngay nếu method != QR.
 *
 * QUAN TRỌNG — chống race condition: rule này gọi tryConsume(), gộp "kiểm tra
 * hợp lệ" + "khoá vé lại" thành 1 UPDATE atomic ở tầng database (xem
 * QrTokenRepository.tryConsume). KHÔNG tách thành "đọc rồi ghi sau" — tách
 * riêng sẽ để hở 1 khoảng thời gian đủ để 2 request trùng gần như đồng thời
 * (MQTT gửi trùng gói QoS≥1, ESP32-CAM gửi lặp qrData, mạng client retry...)
 * đều đọc thấy "chưa dùng" trước khi bên nào kịp ghi "đã dùng" — dẫn tới 1
 * vé QR bị dùng được ≥ 2 lần, sai thiết kế "chỉ 1 lần".
 *
 * Vì rule này VỪA kiểm tra VỪA khoá vé luôn (atomic), nó PHẢI là bước quyết
 * định cuối cùng cho câu hỏi "vé có hợp lệ không" — không gọi lại
 * findValid()/tryConsume() ở đâu khác trong luồng QR nữa.
 *
 * Với QR, QrToken.room là bằng chứng phân quyền — ĐỘC LẬP với RoomPermission
 * (dành cho quyền dài hạn). RoomPermissionRule đã SKIP hoàn toàn khi method
 * == QR để khách vãng lai không bị chặn nhầm.
 */
@Component
@RequiredArgsConstructor
public class QrTokenStatusRule implements AccessRule {

    private final QrTokenService qrTokenService;

    @Override
    public String getKey() { return "QR_TOKEN_STATUS"; }

    @Override
    public int getDefaultOrder() { return 10; }

    @Override
    public RuleDecision evaluate(AccessContext context) {
        if (context.getMethod() != AccessMethod.QR) return RuleDecision.SKIP;

        String token = context.getCredentialData();
        if (token == null) return RuleDecision.DENY;

        Long deviceRoomId = context.getDevice().getRoom().getId();

        // 1 lệnh UPDATE atomic duy nhất: đúng token + chưa dùng + còn hạn + đúng
        // phòng, TẤT CẢ trong 1 giao dịch không thể chen ngang. true = request
        // này "giật" được vé; false = trễ tay (đã dùng/hết hạn/sai phòng/không
        // tồn tại — kể cả trường hợp do 1 request KHÁC vừa giật mất).
        boolean consumed = qrTokenService.tryConsume(token, deviceRoomId);
        return consumed ? RuleDecision.SKIP : RuleDecision.DENY;
    }

    @Override
    public String denyMessage() { return "Mã QR không hợp lệ, đã hết hạn, sai phòng, hoặc đã được sử dụng"; }
}
