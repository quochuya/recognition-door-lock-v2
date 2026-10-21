package com.example.lock.access.rule;

import com.example.lock.access.result.RuleDecision;

/**
 * Thêm rule mới: tạo class implements AccessRule trong access.rule.impl,
 * đánh dấu @Component. AccessEngine tự nhặt vào, không cần sửa gì khác.
 */
public interface AccessRule {

    String getKey();

    default int getDefaultOrder() {
        return 100;
    }

    RuleDecision evaluate(AccessContext context);

    default String denyMessage() {
        return "Bị từ chối bởi quy tắc " + getKey();
    }
}
