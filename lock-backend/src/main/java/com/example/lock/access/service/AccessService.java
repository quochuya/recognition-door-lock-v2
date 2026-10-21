package com.example.lock.access.service;

import com.example.lock.access.entity.AccessLog;
import com.example.lock.access.entity.AccessMethod;
import com.example.lock.access.result.AccessResult;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.card.entity.RfidCard;
import com.example.lock.mqtt.LockCommandPublisher;
import com.example.lock.repository.AccessLogRepository;
import com.example.lock.repository.LockDeviceRepository;
import com.example.lock.repository.RfidCardRepository;
import com.example.lock.room.entity.LockDevice;
import com.example.lock.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessService {

    private final AccessEngine accessEngine;
    private final AccessLogRepository accessLogRepository;
    private final LockDeviceRepository lockDeviceRepository;
    private final RfidCardRepository rfidCardRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final LockCommandPublisher lockCommandPublisher;
    private final QrTokenService qrTokenService;

    /** Gọi từ DeviceMessageListener khi có sự kiện quẹt thẻ qua MQTT */
    @Transactional
    public AccessResult handleCardScan(String deviceCode, String cardUid) {
        LockDevice device = lockDeviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị: " + deviceCode));

        RfidCard card = rfidCardRepository.findByUid(cardUid).orElse(null);
        User user = card != null ? card.getOwner() : null;

        AccessContext context = AccessContext.builder()
                .device(device).user(user).card(card)
                .method(AccessMethod.RFID)
                .timestamp(LocalDateTime.now())
                .adminOverride(false)
                .build();

        AccessResult result = accessEngine.run(context);
        persistAndBroadcast(context, result);

        if (result.isAllowed()) {
            unlockPhysically(device);
            if (card != null) {
                card.setLastUsedAt(LocalDateTime.now());
                card.setAccessCount(card.getAccessCount() + 1);
                rfidCardRepository.save(card);
            }
        } else {
            lockCommandPublisher.publishDenied(deviceCode, result.getReason());
        }
        return result;
    }

    /**
     * Gọi từ DeviceMessageListener khi ESP32-CAM báo quét được QR qua MQTT
     * (topic lock/{deviceCode}/qr-attempt). Cùng 1 rule engine với RFID —
     * chỉ khác ở chỗ user được resolve từ QrToken thay vì từ RfidCard.
     */
    @Transactional
    public AccessResult handleQrScan(String deviceCode, String qrData) {
        LockDevice device = lockDeviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị: " + deviceCode));

        // CHỈ ĐỌC, mang tính suy đoán — dùng để biết user là ai trước khi dựng
        // AccessContext (TimeWindowRule cần role của user). ĐÂY KHÔNG PHẢI quyết
        // định cuối cùng — quyết định thật (atomic, chống race condition) nằm
        // trong QrTokenStatusRule.tryConsume(), chạy bên trong accessEngine.run().
        // Nếu 2 request trùng nhau chạy tới đây gần như đồng thời, cả 2 có thể
        // cùng đọc được user ở bước này — không sao, vì bước "khoá vé" atomic
        // phía sau sẽ tự đảm bảo chỉ 1 trong 2 được ALLOW thật sự.
        var qrToken = qrTokenService.findValid(qrData);
        User user = qrToken != null ? qrToken.getUser() : null;

        AccessContext context = AccessContext.builder()
                .device(device).user(user).card(null)
                .method(AccessMethod.QR)
                .timestamp(LocalDateTime.now())
                .adminOverride(false)
                .credentialData(qrData)
                .build();

        AccessResult result = accessEngine.run(context);
        persistAndBroadcast(context, result);

        if (result.isAllowed()) {
            // Vé đã được khoá lại (used=true) ngay trong QrTokenStatusRule.tryConsume()
            // — atomic, cùng lúc với việc xác nhận hợp lệ. KHÔNG gọi markUsed() ở đây
            // nữa: tách thành bước riêng sau khi đã ALLOW chính là nguyên nhân gây
            // race condition trước đó (khoảng hở giữa "biết là ALLOW" và "khoá vé").
            unlockPhysically(device);
        } else {
            lockCommandPublisher.publishDenied(deviceCode, result.getReason());
        }
        return result;
    }

    /**
     * Ghi log BÙ cho 1 lượt mở cửa mà ESP32 đã tự quyết định lúc mất mạng
     * (dựa trên whitelist cache local). KHÔNG chạy lại rule engine — quyết
     * định "cho vào" đã xảy ra thật ngoài đời, ở đây chỉ đồng bộ lại lịch sử.
     *
     * FIX #3 (chống ghi trùng): ESP32 có thể gửi lại CÙNG 1 batch nếu chưa
     * nhận được ACK (xem DeviceMessageListener + LockCommandPublisher.publishOfflineAck)
     * — kiểm tra đã có log tương tự trong cửa sổ ±2 giây trước khi insert,
     * tránh đếm trùng 1 lượt mở cửa thật thành 2 lượt trong báo cáo.
     */
    @Transactional
    public void logOfflineScan(String deviceCode, String cardUid, LocalDateTime occurredAt) {
        LockDevice device = lockDeviceRepository.findByDeviceCode(deviceCode).orElse(null);
        if (device == null) return; // thiết bị lạ, bỏ qua thay vì ném lỗi làm rớt cả batch

        RfidCard card = rfidCardRepository.findByUid(cardUid).orElse(null);
        User user = card != null ? card.getOwner() : null;

        LocalDateTime from = occurredAt.minusSeconds(2);
        LocalDateTime to = occurredAt.plusSeconds(2);
        if (accessLogRepository.existsSimilarOfflineLog(device, user, from, to)) {
            log.info("Bo qua offline-event trung lap: device={} uid={} time={}", deviceCode, cardUid, occurredAt);
            return;
        }

        AccessLog savedLog = accessLogRepository.save(AccessLog.builder()
                .lockDevice(device)
                .user(user)
                .method(AccessMethod.RFID)
                .success(true)
                .denyReason(null)
                .offline(true)
                .occurredAt(occurredAt)
                .build());

        if (card != null) {
            if (card.getLastUsedAt() == null || occurredAt.isAfter(card.getLastUsedAt())) {
                card.setLastUsedAt(occurredAt);
            }
            card.setAccessCount(card.getAccessCount() + 1);
            rfidCardRepository.save(card);
        }

        String roomLabel = "Phòng " + device.getRoom().getRoomNumber();
        messagingTemplate.convertAndSend("/topic/access-logs", AccessLogEvent.builder()
                .deviceCode(device.getDeviceCode())
                .roomLabel(roomLabel)
                .userName(user != null ? user.getFullName() : "Không xác định")
                .method(AccessMethod.RFID.name())
                .success(true)
                .reason(null)
                .occurredAt(savedLog.getOccurredAt())
                .offline(true)
                .build());
    }

    /** Gọi từ LockController khi admin bấm Khóa/Mở cửa trên dashboard */
    @Transactional
    public AccessResult handleRemoteControl(Long lockDeviceId, User admin, boolean unlock) {
        LockDevice device = lockDeviceRepository.findById(lockDeviceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị id=" + lockDeviceId));

        AccessContext context = AccessContext.builder()
                .device(device).user(admin).card(null)
                .method(AccessMethod.APP)
                .timestamp(LocalDateTime.now())
                .adminOverride(true)
                .build();

        AccessResult result = accessEngine.run(context);
        persistAndBroadcast(context, result);

        if (result.isAllowed()) {
            if (unlock) unlockPhysically(device); else lockPhysically(device);
        }
        return result;
    }

    private void persistAndBroadcast(AccessContext context, AccessResult result) {
        AccessLog log = accessLogRepository.save(AccessLog.builder()
                .lockDevice(context.getDevice())
                .user(context.getUser())
                .method(context.getMethod())
                .success(result.isAllowed())
                .denyReason(result.isAllowed() ? null : result.getRuleKey())
                .occurredAt(context.getTimestamp())
                .build());

        String roomLabel = "Phòng " + log.getLockDevice().getRoom().getRoomNumber();
        messagingTemplate.convertAndSend("/topic/access-logs", AccessLogEvent.builder()
                .deviceCode(log.getLockDevice().getDeviceCode())
                .roomLabel(roomLabel)
                .userName(log.getUser() != null ? log.getUser().getFullName() : "Không xác định")
                .method(log.getMethod().name())
                .success(log.isSuccess())
                .reason(log.getDenyReason())
                .occurredAt(log.getOccurredAt())
                .build());
    }

    private void unlockPhysically(LockDevice device) {
        device.setLockState(LockDevice.LockState.UNLOCKED);
        lockDeviceRepository.save(device);
        lockCommandPublisher.publishCommand(device.getDeviceCode(), "UNLOCK");
    }

    private void lockPhysically(LockDevice device) {
        device.setLockState(LockDevice.LockState.LOCKED);
        lockDeviceRepository.save(device);
        lockCommandPublisher.publishCommand(device.getDeviceCode(), "LOCK");
    }
}
