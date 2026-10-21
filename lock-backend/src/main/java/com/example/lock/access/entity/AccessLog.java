package com.example.lock.access.entity;

import com.example.lock.room.entity.LockDevice;
import com.example.lock.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lock_device_id")
    private LockDevice lockDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccessMethod method;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 40)
    private String denyReason;

    /** true = ghi bù từ hàng đợi offline của ESP32 (không phải quyết định real-time từ rule engine) */
    @Column(nullable = false)
    @Builder.Default
    private boolean offline = false;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) occurredAt = LocalDateTime.now();
    }
}
