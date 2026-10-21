package com.example.lock.controller.dto;

import com.example.lock.access.entity.RoomPermission;
import com.example.lock.card.entity.RfidCard;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CardDto {
    private Long id;
    private String uid;
    private Long ownerId;
    private String ownerName;
    private String ownerRole;
    private RfidCard.CardStatus status;
    private String roomLabel; // vd "Phòng 101, Phòng 205" — rỗng nếu chưa gán quyền phòng nào
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private LocalDateTime lastUsedAt;
    private Integer accessCount;

    public static CardDto from(RfidCard card, List<RoomPermission> permissions) {
        String roomLabel = permissions.isEmpty() ? "—" :
                permissions.stream()
                        .map(p -> "Phòng " + p.getRoom().getRoomNumber())
                        .distinct()
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("—");

        return CardDto.builder()
                .id(card.getId())
                .uid(card.getUid())
                .ownerId(card.getOwner().getId())
                .ownerName(card.getOwner().getFullName())
                .ownerRole(card.getOwner().getRole().name())
                .status(card.getStatus())
                .roomLabel(roomLabel)
                .issuedAt(card.getIssuedAt())
                .expiresAt(card.getExpiresAt())
                .lastUsedAt(card.getLastUsedAt())
                .accessCount(card.getAccessCount())
                .build();
    }
}
