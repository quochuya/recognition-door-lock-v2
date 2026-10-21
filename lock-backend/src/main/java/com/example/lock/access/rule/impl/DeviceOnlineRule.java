package com.example.lock.access.rule.impl;

import com.example.lock.access.result.RuleDecision;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.access.rule.AccessRule;
import com.example.lock.room.entity.LockDevice;
import org.springframework.stereotype.Component;

@Component
public class DeviceOnlineRule implements AccessRule {

    @Override
    public String getKey() { return "DEVICE_ONLINE"; }

    @Override
    public int getDefaultOrder() { return 5; }

    @Override
    public RuleDecision evaluate(AccessContext context) {
        if (!context.isAdminOverride()) return RuleDecision.SKIP; // quẹt thẻ tại chỗ thì chắc chắn online

        LockDevice device = context.getDevice();
        return device.getConnectionStatus() == LockDevice.ConnectionStatus.ONLINE
                ? RuleDecision.SKIP
                : RuleDecision.DENY;
    }

    @Override
    public String denyMessage() { return "Thiết bị đang mất kết nối, không thể gửi lệnh"; }
}
