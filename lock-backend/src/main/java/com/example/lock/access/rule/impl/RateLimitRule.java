package com.example.lock.access.rule.impl;

import com.example.lock.access.result.RuleDecision;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.access.rule.AccessRule;
import com.example.lock.repository.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RateLimitRule implements AccessRule {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 10;

    private final AccessLogRepository accessLogRepository;

    @Override
    public String getKey() { return "RATE_LIMIT"; }

    @Override
    public int getDefaultOrder() { return 1; }

    @Override
    public RuleDecision evaluate(AccessContext context) {
        if (context.isAdminOverride()) return RuleDecision.SKIP;

        LocalDateTime since = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
        long recentFailures = accessLogRepository
                .countByLockDeviceIdAndSuccessFalseAndOccurredAtAfter(context.getDevice().getId(), since);

        return recentFailures >= MAX_FAILED_ATTEMPTS ? RuleDecision.DENY : RuleDecision.SKIP;
    }

    @Override
    public String denyMessage() {
        return "Quá nhiều lượt thất bại — tạm khoá " + WINDOW_MINUTES + " phút";
    }
}
