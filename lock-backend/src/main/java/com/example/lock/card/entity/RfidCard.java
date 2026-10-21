package com.example.lock.card.entity;

import com.example.lock.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfid_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfidCard {

    public enum CardStatus { ACTIVE, BLOCKED, EXPIRING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private LocalDateTime lastUsedAt;

    @Builder.Default
    private Integer accessCount = 0;
}
