package com.example.lock.controller.dto;

import com.example.lock.access.entity.QrToken;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QrTokenDto {
    private Long id;
    private String token;       // nội dung để mã hoá thành ảnh QR
    private String userName;
    private Integer roomNumber;
    private LocalDateTime expiresAt;

    public static QrTokenDto from(QrToken t) {
        return QrTokenDto.builder()
                .id(t.getId())
                .token(t.getToken())
                .userName(t.getUser().getFullName())
                .roomNumber(t.getRoom().getRoomNumber())
                .expiresAt(t.getExpiresAt())
                .build();
    }
}
