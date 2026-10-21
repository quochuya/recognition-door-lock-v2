package com.example.lock.device.controller;

import com.example.lock.controller.dto.DeviceDetailDto;
import com.example.lock.controller.dto.DeviceDto;
import com.example.lock.controller.dto.RegisterDeviceRequest;
import com.example.lock.controller.dto.UpdateDeviceRequest;
import com.example.lock.device.service.DeviceService;
import com.example.lock.repository.AccessLogRepository;
import com.example.lock.room.entity.LockDevice;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Xem (GET): ADMIN + STAFF. Gắn/gỡ/khởi động lại thiết bị: ADMIN only —
 * đây là thao tác vận hành hạ tầng vật lý, không giao cho STAFF.
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final AccessLogRepository accessLogRepository;


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional(readOnly = true)
    public List<DeviceDto> list(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DeviceService.StatusFilter status) {
        return deviceService.list(buildingId, search, status).stream().map(DeviceDto::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional(readOnly = true)
    public DeviceDetailDto detail(@PathVariable Long id) {
        LockDevice device = deviceService.getOrThrow(id);
        return DeviceDetailDto.from(device, accessLogRepository.findTop5ByLockDeviceIdOrderByOccurredAtDesc(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceDto register(@Valid @RequestBody RegisterDeviceRequest req) {
        return DeviceDto.from(deviceService.register(req.getDeviceCode(), req.getRoomId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceDto updateRoom(@PathVariable Long id, @Valid @RequestBody UpdateDeviceRequest req) {
        return DeviceDto.from(deviceService.updateRoom(id, req.getRoomId()));
    }

    @PostMapping("/{id}/restart")
    @PreAuthorize("hasRole('ADMIN')")
    public void restart(@PathVariable Long id) {
        deviceService.restart(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void remove(@PathVariable Long id) {
        deviceService.remove(id);
    }
}
