package com.example.lock.access.rule.impl;

import com.example.lock.access.entity.AccessRuleConfig;
import com.example.lock.access.result.RuleDecision;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.access.rule.AccessRule;
import com.example.lock.repository.AccessRuleConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cấu hình qua bảng access_rule_configs, ruleKey=TIME_WINDOW, paramsJson vd:
 *   {"start":"06:00","end":"22:00","restrictedRoles":["GUEST"]}
 */
@Component
@RequiredArgsConstructor
public class TimeWindowRule implements AccessRule {

    private final AccessRuleConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getKey() { return "TIME_WINDOW"; }

    @Override
    public int getDefaultOrder() { return 30; }

    @Override
    @SuppressWarnings("unchecked")
    public RuleDecision evaluate(AccessContext context) {
        if (context.getUser() == null) return RuleDecision.SKIP;

        AccessRuleConfig config = configRepository.findByRuleKey(getKey()).orElse(null);
        if (config == null || !config.isEnabled() || config.getParamsJson() == null) {
            return RuleDecision.SKIP;
        }

        try {
            Map<String, Object> params = objectMapper.readValue(config.getParamsJson(), Map.class);
            List<String> restrictedRoles = (List<String>) params.getOrDefault("restrictedRoles", List.of());
            Set<String> roles = Set.copyOf(restrictedRoles);

            if (!roles.contains(context.getUser().getRole().name())) return RuleDecision.SKIP;

            LocalTime start = LocalTime.parse((String) params.get("start"));
            LocalTime end = LocalTime.parse((String) params.get("end"));
            LocalTime now = context.getTimestamp().toLocalTime();

            return (!now.isBefore(start) && !now.isAfter(end)) ? RuleDecision.SKIP : RuleDecision.DENY;
        } catch (Exception e) {
            return RuleDecision.SKIP; // cấu hình lỗi -> fail-open, nên log cảnh báo riêng
        }
    }

    @Override
    public String denyMessage() { return "Ngoài khung giờ được phép truy cập"; }
}
