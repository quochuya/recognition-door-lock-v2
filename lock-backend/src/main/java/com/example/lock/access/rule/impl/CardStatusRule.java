package com.example.lock.access.rule.impl;

import com.example.lock.access.entity.AccessMethod;
import com.example.lock.access.result.RuleDecision;
import com.example.lock.access.rule.AccessContext;
import com.example.lock.access.rule.AccessRule;
import com.example.lock.card.entity.RfidCard;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CardStatusRule implements AccessRule {

    @Override
    public String getKey() { return "CARD_STATUS"; }

    @Override
    public int getDefaultOrder() { return 10; }

    @Override
    public RuleDecision evaluate(AccessContext context) {
        if (context.getMethod() != AccessMethod.RFID) return RuleDecision.SKIP;

        RfidCard card = context.getCard();
        if (card == null) return RuleDecision.DENY;
        if (card.getStatus() == RfidCard.CardStatus.BLOCKED) return RuleDecision.DENY;
        if (card.getExpiresAt() != null && card.getExpiresAt().isBefore(LocalDate.now())) {
            return RuleDecision.DENY;
        }
        return RuleDecision.SKIP;
    }

    @Override
    public String denyMessage() { return "Thẻ RFID đã bị khóa hoặc hết hạn"; }
}
