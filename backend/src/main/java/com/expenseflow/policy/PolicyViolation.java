package com.expenseflow.policy;

/**
 * A single policy rule failure. {@code rule} identifies the rule that failed,
 * {@code message} is a human-readable explanation, and {@code lineItemId} is the
 * offending line item when the violation is item-specific (null for report-level rules).
 */
public record PolicyViolation(String rule, String message, Long lineItemId) {

    public static PolicyViolation report(String rule, String message) {
        return new PolicyViolation(rule, message, null);
    }

    public static PolicyViolation item(String rule, String message, Long lineItemId) {
        return new PolicyViolation(rule, message, lineItemId);
    }
}
