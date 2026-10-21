package com.example.lock.access.service;

import com.example.lock.access.entity.AccessRuleConfig;
import com.example.lock.access.result.AccessResult;
import com.example.lock.access.result.RuleDecision;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.access.rule.AccessRule;
import com.example.lock.repository.AccessRuleConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class AccessEngine {

    private final List<AccessRule> rules;
    private final AccessRuleConfigRepository configRepository;

    private List<AccessRule> orderedRules;

    @PostConstruct
    void init() {
        reorder();
    }

    /** Gọi lại khi admin đổi priority/enabled trong AccessRuleConfig từ dashboard */
    public void reorder() {
        Map<String, AccessRuleConfig> configByKey = configRepository.findAll().stream()
                .collect(Collectors.toMap(AccessRuleConfig::getRuleKey, c -> c));

        this.orderedRules = rules.stream()
                .filter(r -> configByKey.getOrDefault(r.getKey(), defaultEnabled()).isEnabled())
                .sorted(Comparator.comparingInt(r ->
                        configByKey.containsKey(r.getKey())
                                ? configByKey.get(r.getKey()).getPriority()
                                : r.getDefaultOrder()))
                .toList();
    }

    private AccessRuleConfig defaultEnabled() {
        return AccessRuleConfig.builder().enabled(true).build();
    }

    public AccessResult run(AccessContext context) {
        for (AccessRule rule : orderedRules) {
            RuleDecision decision = rule.evaluate(context);
            if (decision == RuleDecision.DENY) return AccessResult.deny(rule.getKey(), rule.denyMessage());
            if (decision == RuleDecision.ALLOW) return AccessResult.allow(rule.getKey());
        }
        return AccessResult.allow("DEFAULT");
    }
}
