package com.example.lock.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lock_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockDevice {

    public enum LockState { LOCKED, UNLOCKED }
    public enum ConnectionStatus { ONLINE, OFFLINE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dùng làm segment topic MQTT: lock/{deviceCode}/... */
    @Column(nullable = false, unique = true, length = 60)
    private String deviceCode;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private LockState lockState = LockState.LOCKED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;

    @Builder.Default
    private Integer batteryPercent = 100;

    private Integer signalPercent;

    @Column(length = 20)
    private String firmwareVersion;

    private LocalDateTime lastSeenAt;

    private LocalDateTime installedAt;

    public void markOnline(int battery, Integer signal, String firmware) {
        this.connectionStatus = ConnectionStatus.ONLINE;
        this.batteryPercent = battery;
        if (signal != null) this.signalPercent = signal;
        if (firmware != null) this.firmwareVersion = firmware;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void markOffline() {
        this.connectionStatus = ConnectionStatus.OFFLINE;
        this.signalPercent = null;
    } 
}
