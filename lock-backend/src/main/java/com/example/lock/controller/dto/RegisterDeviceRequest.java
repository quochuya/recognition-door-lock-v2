package com.example.lock.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceRequest {
    @NotBlank
    private String deviceCode;
    @NotNull
    private Long roomId;
}
