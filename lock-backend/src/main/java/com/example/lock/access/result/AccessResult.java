package com.example.lock.access.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccessResult {

    private final boolean allowed;
    private final String ruleKey;
    private final String reason;

    public static AccessResult allow(String ruleKey) {
        return AccessResult.builder().allowed(true).ruleKey(ruleKey).reason("OK").build();
    }

    public static AccessResult deny(String ruleKey, String reason) {
        return AccessResult.builder().allowed(false).ruleKey(ruleKey).reason(reason).build();
    }
}
