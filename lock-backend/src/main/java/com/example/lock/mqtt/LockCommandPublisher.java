package com.example.lock.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LockCommandPublisher {

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishCommand(String deviceCode, String command) {
        publish("lock/" + deviceCode + "/command", Map.of("command", command));
    }

    public void publishRestart(String deviceCode) {
        publish("lock/" + deviceCode + "/command", Map.of("command", "RESTART"));
    }

    public void publishDenied(String deviceCode, String reason) {
        publish("lock/" + deviceCode + "/result", Map.of("allow", false, "reason", reason));
    }

    /** Đẩy danh sách UID RFID hợp lệ xuống thiết bị — ESP32 lưu vào NVS để dùng khi mất mạng */
    public void publishWhitelist(String deviceCode, java.util.List<String> uids) {
        publish("lock/" + deviceCode + "/whitelist", Map.of("uids", uids));
    }

    /**
     * FIX #2 (chống mất dữ liệu hàng đợi offline): xác nhận đã xử lý XONG toàn
     * bộ batch offline-events. ESP32 CHỈ xoá hàng đợi cục bộ khi nhận được ACK
     * này — không xoá ngay khi publish() lên MQTT trả về true (điều đó chỉ có
     * nghĩa "đã gửi xuống mạng", không đảm bảo backend đã nhận/xử lý xong).
     */
    public void publishOfflineAck(String deviceCode) {
        publish("lock/" + deviceCode + "/offline-events-ack", Map.of("ack", true));
    }

    private void publish(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            mqttOutboundChannel.send(MessageBuilder.withPayload(json)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Không publish được MQTT topic=" + topic, e);
        }
    }
}
