package com.example.lock.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuildingRequest {
    @NotBlank
    private String name;
    private String address;
}
