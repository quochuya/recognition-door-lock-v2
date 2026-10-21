package com.example.lock.access.service;

import com.example.lock.access.entity.QrToken;
import com.example.lock.repository.QrTokenRepository;
import com.example.lock.repository.RoomRepository;
import com.example.lock.repository.UserRepository;
import com.example.lock.room.entity.Room;
import com.example.lock.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class QrTokenService {

    private final QrTokenRepository qrTokenRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Sinh 1 vé QR mới cho user vào 1 phòng cụ thể, hạn dùng validityMinutes phút.
     * Chuỗi trả về chính là nội dung sẽ được mã hoá thành ảnh QR ở frontend
     * (dùng thư viện JS như qrcode.react — backend chỉ cần trả chuỗi text thô).
     */
    @Transactional
    public QrToken generate(Long userId, Long roomId, int validityMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id=" + userId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng id=" + roomId));

        byte[] randomBytes = new byte[24]; // 24 byte -> ~32 ký tự base64, đủ dài để không đoán được
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        return qrTokenRepository.save(QrToken.builder()
                .token(token)
                .user(user)
                .room(room)
                .expiresAt(LocalDateTime.now().plusMinutes(validityMinutes))
                .used(false)
                .build());
    }

    /**
     * Dùng bởi QrTokenService.findValid() để dựng AccessContext.user TRƯỚC khi
     * chạy rule engine (TimeWindowRule cần biết role của user). Đây CHỈ LÀ
     * ĐỌC — không phải quyết định cuối cùng. Quyết định thật (atomic, chống
     * race condition) nằm ở tryConsume() bên dưới, chạy trong QrTokenStatusRule.
     */
    public QrToken findValid(String token) {
        return qrTokenRepository.findByToken(token)
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    /**
     * Kiểm tra hợp lệ + khoá vé lại trong ĐÚNG 1 giao dịch atomic (xem
     * QrTokenRepository.tryConsume) — đây mới là nguồn quyết định thật sự,
     * gọi bên trong QrTokenStatusRule. Trả về true nếu request này "giật"
     * được vé thành công (tức là request hợp lệ DUY NHẤT được đi tiếp).
     */
    @Transactional
    public boolean tryConsume(String token, Long roomId) {
        int updated = qrTokenRepository.tryConsume(token, roomId, LocalDateTime.now());
        return updated == 1;
    }
}
