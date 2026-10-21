package com.example.lock.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDeviceRequest {
    @NotNull
    private Long roomId;
}
