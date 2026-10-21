package com.example.lock.repository;

import com.example.lock.access.entity.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface QrTokenRepository extends JpaRepository<QrToken, Long> {
    Optional<QrToken> findByToken(String token);

    /**
     * Gộp "kiểm tra hợp lệ" + "khoá vé lại" thành 1 UPDATE duy nhất, atomic ở
     * tầng database — không có khoảng hở giữa đọc và ghi như tách riêng 2 bước
     * (findValid rồi markUsed), nên không thể bị race condition khi 2 request
     * trùng gần như đồng thời (MQTT gửi trùng gói QoS≥1, ESP32-CAM gửi lặp,
     * mạng chập chờn client retry...).
     *
     * KHÔNG dùng clearAutomatically=true: từng thêm vào để phòng đọc-cũ (stale
     * read) nếu có chỗ nào đọc lại QrToken sau update trong cùng transaction —
     * nhưng thực tế không có chỗ nào làm vậy, trong khi clearAutomatically lại
     * xoá sạch TOÀN BỘ persistence context (không chỉ riêng QrToken), làm
     * "mồ côi" các entity khác đã load trước đó trong cùng transaction (vd
     * LockDevice.room load kiểu lazy trong AccessService.handleQrScan) —
     * gây LazyInitializationException khi truy cập lại sau đó. Đổi lại thành
     * lỗi thật, nghiêm trọng hơn hẳn tình huống lý thuyết ban đầu định phòng.
     *
     * Trả về số dòng bị ảnh hưởng: 1 = thành công (vé vừa được "giật" bởi
     * đúng request này), 0 = thất bại (đã dùng / hết hạn / sai phòng / không
     * tồn tại — có thể do 1 request KHÁC đã "giật" mất ngay trước đó).
     */
    @Modifying
    @Query("""
        update QrToken t set t.used = true
        where t.token = :token
          and t.used = false
          and t.expiresAt > :now
          and t.room.id = :roomId
        """)
    int tryConsume(@Param("token") String token, @Param("roomId") Long roomId, @Param("now") LocalDateTime now);
}
