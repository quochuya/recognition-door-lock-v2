package com.example.lock.mqtt;

import com.example.lock.access.service.AccessService;
import com.example.lock.access.service.WhitelistService;
import com.example.lock.card.service.CardEnrollmentService;
import com.example.lock.repository.LockDeviceRepository;
import com.example.lock.repository.RfidCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceMessageListener {

    private final AccessService accessService;
    private final LockDeviceRepository lockDeviceRepository;
    private final RfidCardRepository rfidCardRepository;
    private final CardEnrollmentService cardEnrollmentService;
    private final WhitelistService whitelistService;
    private final LockCommandPublisher lockCommandPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handle(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        String payload = message.getPayload().toString();
        if (topic == null) return;

        String[] parts = topic.split("/"); // lock/{deviceCode}/{event}
        if (parts.length != 3) {
            log.warn("Bỏ qua topic không đúng định dạng: {}", topic);
            return;
        }
        String deviceCode = parts[1];
        String event = parts[2];

        try {
            // "whitelist-request" thường gửi payload rỗng -> không cần parse JSON
            if (event.equals("whitelist-request")) {
                log.info("Thiết bị {} xin lại whitelist", deviceCode);
                whitelistService.publishOnRequest(deviceCode);
                return;
            }

            JsonNode json = objectMapper.readTree(payload);
            switch (event) {
                case "access-attempt" -> handleAccessAttempt(deviceCode, json);
                case "qr-attempt" -> handleQrAttempt(deviceCode, json);
                case "status" -> handleStatus(deviceCode, json);
                case "offline-events" -> handleOfflineEvents(deviceCode, json);
                default -> log.warn("Sự kiện không xác định: {}", event);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý MQTT topic={} payload={}", topic, payload, e);
        }
    }

    private void handleAccessAttempt(String deviceCode, JsonNode json) {
        String cardUid = json.path("cardUid").asText(null);
        if (cardUid == null) return;

        boolean cardKnown = rfidCardRepository.findByUid(cardUid).isPresent();

        if (!cardKnown && cardEnrollmentService.isEnrollmentActive()) {
            cardEnrollmentService.captureUnknownScan(cardUid, deviceCode);
            return;
        }

        accessService.handleCardScan(deviceCode, cardUid);
    }

    /** ESP32-CAM báo về khi quét được QR: {"qrData": "..."} */
    private void handleQrAttempt(String deviceCode, JsonNode json) {
        String qrData = json.path("qrData").asText(null);
        if (qrData == null) return;
        accessService.handleQrScan(deviceCode, qrData);
    }

    /**
     * ESP32 gửi lên khi vừa kết nối lại sau 1 khoảng thời gian offline:
     *   {"events": [{"cardUid":"...", "occurredAt":"2026-07-23T10:15:30"}, ...]}
     *
     * FIX #1 (chống mất dữ liệu vĩnh viễn): nếu ESP32 chưa từng đồng bộ NTP,
     * occurredAt sẽ có dạng "unsynced-<millis>" — KHÔNG parse được ISO-8601.
     * Trước đây bị bỏ qua âm thầm (mất log). Giờ dùng giờ server hiện tại làm
     * phương án dự phòng — chấp nhận sai lệch thời gian còn hơn mất log.
     *
     * FIX #2 (chống mất hàng đợi offline phía ESP32): chỉ gửi ACK SAU KHI đã
     * xử lý xong toàn bộ batch — ESP32 chỉ xoá hàng đợi cục bộ khi nhận được
     * ACK này (xem esp32 firmware), không xoá ngay khi publish() thành công
     * (publish() chỉ đảm bảo gửi xuống mạng, không đảm bảo backend đã xử lý).
     */
    private void handleOfflineEvents(String deviceCode, JsonNode json) {
        JsonNode events = json.path("events");
        if (!events.isArray()) return;

        int count = 0;
        for (JsonNode ev : events) {
            String cardUid = ev.path("cardUid").asText(null);
            String occurredAtStr = ev.path("occurredAt").asText(null);
            if (cardUid == null || occurredAtStr == null) continue;

            LocalDateTime occurredAt;
            try {
                occurredAt = LocalDateTime.parse(occurredAtStr);
            } catch (Exception parseEx) {
                occurredAt = LocalDateTime.now();
                log.warn("Offline-event từ {} có timestamp không hợp lệ ('{}') — ESP32 chưa từng đồng bộ NTP, dùng giờ server thay thế",
                        deviceCode, occurredAtStr);
            }

            try {
                accessService.logOfflineScan(deviceCode, cardUid, occurredAt);
                count++;
            } catch (Exception e) {
                log.warn("Bỏ qua 1 offline-event không hợp lệ từ {}: {}", deviceCode, ev, e);
            }
        }
        log.info("Đã đồng bộ {} sự kiện offline từ thiết bị {}", count, deviceCode);

        // Xử lý xong toàn bộ batch (kể cả khi có lỗi từng dòng riêng lẻ) mới ACK.
        lockCommandPublisher.publishOfflineAck(deviceCode);
    }

    @Transactional
    private void handleStatus(String deviceCode, JsonNode json) {
        lockDeviceRepository.findByDeviceCode(deviceCode).ifPresent(device -> {
            int battery = json.path("battery").asInt(device.getBatteryPercent());
            Integer signal = json.has("signal") ? json.path("signal").asInt() : null;
            String firmware = json.path("firmware").asText(null);
            device.markOnline(battery, signal, firmware);
            lockDeviceRepository.save(device);
        });
    }
}
