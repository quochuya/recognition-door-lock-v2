package com.example.lock.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Quy ước topic (theo device):
 *   lock/{deviceCode}/access-attempt    <- ESP32 (RC522) publish khi quẹt thẻ: {"cardUid":"..."}
 *   lock/{deviceCode}/qr-attempt        <- ESP32-CAM publish khi quét được QR: {"qrData":"..."}
 *   lock/{deviceCode}/status            <- thiết bị publish heartbeat: {"battery":85,"signal":80,"firmware":"..."}
 *   lock/{deviceCode}/whitelist-request <- ESP32 (RFID) publish khi vừa kết nối lại, payload rỗng
 *   lock/{deviceCode}/offline-events    <- ESP32 (RFID) publish sự kiện bù sau khi mất mạng
 *   lock/{deviceCode}/command           -> backend publish lệnh: {"command":"UNLOCK"} | {"command":"LOCK"} | {"command":"RESTART"}
 *   lock/{deviceCode}/result            -> backend publish kết quả bị từ chối: {"allow":false,"reason":"..."}
 *   lock/{deviceCode}/whitelist         -> backend publish UID hợp lệ để ESP32 (RFID) cache offline: {"uids":["..."]}
 *   lock/{deviceCode}/offline-events-ack -> backend xác nhận đã xử lý xong batch offline-events
 *
 * QR KHÔNG có whitelist/offline-events — ESP32-CAM chỉ publish qr-attempt và
 * subscribe command/result, không cache/không hoạt động offline (xem lý do
 * trong WhitelistService/QrTokenStatusRule).
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:lock-backend}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        var options = new org.eclipse.paho.client.mqttv3.MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        if (!username.isBlank()) options.setUserName(username);
        if (!password.isBlank()) options.setPassword(password.toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }

    // ---------- Inbound: device -> server ----------

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter() {
        var adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-in", mqttClientFactory(),
                "lock/+/access-attempt", "lock/+/qr-attempt", "lock/+/status",
                "lock/+/whitelist-request", "lock/+/offline-events");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInboundHandler(DeviceMessageListener listener) {
        return listener::handle;
    }

    // ---------- Outbound: server -> device (đây là phần "miệng nói" cần cho lệnh LOCK/UNLOCK) ----------

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "-out", mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }
}
