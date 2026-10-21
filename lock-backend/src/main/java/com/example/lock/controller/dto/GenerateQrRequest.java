package com.example.lock.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateQrRequest {
    @NotNull
    private Long userId;
    @NotNull
    private Long roomId;
    @Min(1) @Max(60)
    private int validityMinutes = 5; // mặc định 5 phút, đủ để quét ngay không cần chờ lâu
}
