package com.example.lock.schedule;

import com.example.lock.repository.LockDeviceRepository;
import com.example.lock.room.entity.LockDevice;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeviceHealthScheduler {

    private static final int HEARTBEAT_TIMEOUT_MINUTES = 5;

    private final LockDeviceRepository lockDeviceRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void checkOfflineDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(HEARTBEAT_TIMEOUT_MINUTES);
        lockDeviceRepository.findAll().stream()
                .filter(d -> d.getConnectionStatus() == LockDevice.ConnectionStatus.ONLINE)
                .filter(d -> d.getLastSeenAt() == null || d.getLastSeenAt().isBefore(threshold))
                .forEach(d -> {
                    d.markOffline();
                    lockDeviceRepository.save(d);
                });
    }
}
