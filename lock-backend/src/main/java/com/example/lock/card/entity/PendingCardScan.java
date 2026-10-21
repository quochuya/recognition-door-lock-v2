package com.example.lock.card.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Khi admin bật "chế độ đăng ký thẻ" và có UID chưa tồn tại trong rfid_cards
 * quẹt vào bất kỳ đầu đọc nào, DeviceMessageListener sẽ lưu vào đây thay vì
 * chạy qua AccessEngine — admin sau đó chọn user để gán, hoàn tất "one-touch enrollment".
 */
@Entity
@Table(name = "pending_card_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingCardScan {

    public enum Status { WAITING, ASSIGNED, DISCARDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String uid;

    @Column(length = 60)
    private String deviceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private Status status = Status.WAITING;

    @Column(nullable = false)
    private LocalDateTime scannedAt;

    @PrePersist
    void onCreate() {
        if (scannedAt == null) scannedAt = LocalDateTime.now();
    }
}
