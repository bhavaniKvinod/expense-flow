package com.expenseflow.exception;

import com.expenseflow.policy.PolicyViolation;

import java.util.List;

/**
 * Thrown when an expense report fails one or more policy rules at submission.
 * Carries the full list of violations so the client can show every problem at once.
 */
public class PolicyViolationException extends RuntimeException {

    private final List<PolicyViolation> violations;

    public PolicyViolationException(List<PolicyViolation> violations) {
        super("Report failed " + violations.size() + " policy rule(s)");
        this.violations = violations;
    }

    public List<PolicyViolation> getViolations() {
        return violations;
    }
}
