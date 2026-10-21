package com.example.lock.access.rule;

import com.example.lock.access.entity.AccessMethod;
import com.example.lock.card.entity.RfidCard;
import com.example.lock.room.entity.LockDevice;
import com.example.lock.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AccessContext {
    private final LockDevice device;
    private final User user;
    private final RfidCard card;
    private final AccessMethod method;
    private final LocalDateTime timestamp;
    private final boolean adminOverride;
    /** Dữ liệu thô của phương thức không phải RFID — vd chuỗi token QR. Dùng chung, tái sử dụng cho PIN/phương thức khác sau này. */
    private final String credentialData;
}
