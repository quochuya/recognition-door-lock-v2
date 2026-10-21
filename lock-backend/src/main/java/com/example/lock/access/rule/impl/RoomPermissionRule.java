package com.example.lock.access.rule.impl;

import com.example.lock.access.entity.AccessMethod;
import com.example.lock.access.result.RuleDecision;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.access.rule.AccessRule;
import com.example.lock.repository.RoomPermissionRepository;
import com.example.lock.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RoomPermissionRule implements AccessRule {

    private final RoomPermissionRepository roomPermissionRepository;

    @Override
    public String getKey() { return "ROOM_PERMISSION"; }

    @Override
    public int getDefaultOrder() { return 20; }

    @Override
    public RuleDecision evaluate(AccessContext context) {
        // QR dùng QrToken.room làm bằng chứng phân quyền riêng (đã kiểm tra ở
        // QrTokenStatusRule, chạy trước rule này) — KHÔNG áp bảng RoomPermission
        // (vốn dành cho quyền dài hạn của user thường trú, khách vãng lai
        // thường không có bản ghi nào ở đó). Không SKIP ở đây sẽ khiến khách
        // vãng lai bị từ chối dù vé QR hợp lệ và đúng phòng.
        if (context.getMethod() == AccessMethod.QR) return RuleDecision.SKIP;

        if (context.getUser() == null) return RuleDecision.DENY;
        if (context.getUser().getRole() == Role.ADMIN) return RuleDecision.SKIP;

        Long roomId = context.getDevice().getRoom().getId();
        return roomPermissionRepository
                .findByUserIdAndRoomId(context.getUser().getId(), roomId)
                .filter(p -> p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(p -> RuleDecision.SKIP)
                .orElse(RuleDecision.DENY);
    }

    @Override
    public String denyMessage() { return "Người dùng không có quyền truy cập phòng này"; }
}

