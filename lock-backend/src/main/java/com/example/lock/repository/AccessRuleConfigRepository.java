package com.example.lock.repository;

import com.example.lock.access.entity.AccessRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessRuleConfigRepository extends JpaRepository<AccessRuleConfig, Long> {
    Optional<AccessRuleConfig> findByRuleKey(String ruleKey);
}
