package com.example.lock.access.service;

import com.example.lock.mqtt.LockCommandPublisher;
import com.example.lock.repository.LockDeviceRepository;
import com.example.lock.repository.RfidCardRepository;
import com.example.lock.repository.RoomPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Đẩy "whitelist offline" — danh sách UID RFID hợp lệ — xuống ESP32 gắn ở 1
 * phòng, để thiết bị tự quyết định mở cửa khi mất kết nối MQTT.
 *
 * CHỈ ÁP DỤNG CHO RFID. QR không có whitelist offline — vé QR dùng 1 lần,
 * xác thực bằng UPDATE atomic trên server (QrTokenStatusRule), không thể
 * cache cục bộ mà vẫn đảm bảo "chỉ dùng 1 lần" khi không có kết nối.
 */
@Service
@RequiredArgsConstructor
public class WhitelistService {

    private final RfidCardRepository rfidCardRepository;
    private final RoomPermissionRepository roomPermissionRepository;
    private final LockDeviceRepository lockDeviceRepository;
    private final LockCommandPublisher lockCommandPublisher;

    public void publishForRoom(Long roomId) {
        lockDeviceRepository.findByRoomId(roomId).ifPresent(device -> {
            List<String> uids = rfidCardRepository.findValidUidsForRoom(roomId);
            lockCommandPublisher.publishWhitelist(device.getDeviceCode(), uids);
        });
    }

    /** Gọi khi 1 thẻ thay đổi — đẩy lại whitelist cho phòng mà chủ thẻ đó có quyền */
    public void publishForUserRooms(Long userId) {
        roomPermissionRepository.findByUserId(userId)
                .forEach(p -> publishForRoom(p.getRoom().getId()));
    }

    /** Gọi khi ESP32 khởi động lại / vừa kết nối lại — gửi ngay whitelist hiện tại theo yêu cầu */
    public void publishOnRequest(String deviceCode) {
        lockDeviceRepository.findByDeviceCode(deviceCode).ifPresent(device -> {
            Long roomId = device.getRoom().getId();
            List<String> uids = rfidCardRepository.findValidUidsForRoom(roomId);
            lockCommandPublisher.publishWhitelist(deviceCode, uids);
        });
    }
}
