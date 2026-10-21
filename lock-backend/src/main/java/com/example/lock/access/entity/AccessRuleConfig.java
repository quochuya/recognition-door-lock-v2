package com.example.lock.access.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "access_rule_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String ruleKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;

    @Column(columnDefinition = "TEXT")
    private String paramsJson;
}
