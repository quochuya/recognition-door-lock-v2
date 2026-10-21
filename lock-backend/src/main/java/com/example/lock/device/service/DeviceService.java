package com.example.lock.device.service;

import com.example.lock.access.service.WhitelistService;
import com.example.lock.mqtt.LockCommandPublisher;
import com.example.lock.repository.LockDeviceRepository;
import com.example.lock.repository.RoomRepository;
import com.example.lock.room.entity.LockDevice;
import com.example.lock.room.entity.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final LockDeviceRepository lockDeviceRepository;
    private final RoomRepository roomRepository;
    private final LockCommandPublisher lockCommandPublisher;
    private final WhitelistService whitelistService;

    public enum StatusFilter { ONLINE, OFFLINE, LOW_BATTERY }

    @Transactional
    public LockDevice register(String deviceCode, Long roomId) {
        if (lockDeviceRepository.findByDeviceCode(deviceCode).isPresent()) {
            throw new IllegalStateException("Mã thiết bị đã tồn tại: " + deviceCode);
        }
        if (lockDeviceRepository.findByRoomId(roomId).isPresent()) {
            throw new IllegalStateException("Phòng này đã gắn 1 thiết bị khác");
        }
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng id=" + roomId));

        LockDevice device = lockDeviceRepository.save(LockDevice.builder()
                .deviceCode(deviceCode)
                .room(room)
                .lockState(LockDevice.LockState.LOCKED)
                .connectionStatus(LockDevice.ConnectionStatus.OFFLINE)
                .batteryPercent(100)
                .installedAt(LocalDateTime.now())
                .build());

        whitelistService.publishForRoom(roomId);
        return device;
    }

    public List<LockDevice> list(Long buildingId, String search, StatusFilter statusFilter) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        List<LockDevice> devices = lockDeviceRepository.findFiltered(buildingId, normalizedSearch);

        if (statusFilter == null) return devices;

        return devices.stream().filter(d -> switch (statusFilter) {
            case ONLINE -> d.getConnectionStatus() == LockDevice.ConnectionStatus.ONLINE;
            case OFFLINE -> d.getConnectionStatus() == LockDevice.ConnectionStatus.OFFLINE;
            case LOW_BATTERY -> d.getBatteryPercent() != null && d.getBatteryPercent() < 25;
        }).collect(Collectors.toList());
    }

    public LockDevice getOrThrow(Long id) {
        return lockDeviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị id=" + id));
    }

    @Transactional
    public LockDevice updateRoom(Long id, Long newRoomId) {
        LockDevice device = getOrThrow(id);
        if (lockDeviceRepository.findByRoomId(newRoomId)
                .filter(d -> !d.getId().equals(id))
                .isPresent()) {
            throw new IllegalStateException("Phòng đích đã gắn 1 thiết bị khác");
        }
        Room room = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng id=" + newRoomId));
        device.setRoom(room);
        LockDevice saved = lockDeviceRepository.save(device);
        whitelistService.publishForRoom(newRoomId);
        return saved;
    }

    public void restart(Long id) {
        LockDevice device = getOrThrow(id);
        lockCommandPublisher.publishRestart(device.getDeviceCode());
    }

    @Transactional
    public void remove(Long id) {
        lockDeviceRepository.deleteById(id);
    }
}
