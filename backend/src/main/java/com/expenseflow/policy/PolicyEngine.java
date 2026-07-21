package com.expenseflow.policy;

import com.expenseflow.domain.Currency;
import com.expenseflow.entity.ExpenseLineItem;
import com.expenseflow.entity.ExpenseReport;
import com.expenseflow.entity.PolicyConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enforces company expense policy at submission time. Returns every violation found so the
 * caller can show them all at once. Pure with respect to persistence — reads the report and
 * config, produces a violation list. Reused for both the "dry-run check" endpoint and the
 * actual submit gate.
 */
@Component
public class PolicyEngine {

    static final String RULE_EMPTY = "REPORT_EMPTY";
    static final String RULE_RECEIPT_REQUIRED = "RECEIPT_REQUIRED";
    static final String RULE_TOTAL_CAP = "TOTAL_REPORT_CAP";
    static final String RULE_FUTURE_DATE = "FUTURE_DATE";
    static final String RULE_TOO_OLD = "EXPENSE_TOO_OLD";
    static final String RULE_DUPLICATE = "DUPLICATE_LINE_ITEM";

    private final Clock clock;

    public PolicyEngine(Clock clock) {
        this.clock = clock;
    }

    public List<PolicyViolation> validate(ExpenseReport report, PolicyConfig config) {
        List<PolicyViolation> violations = new ArrayList<>();
        List<ExpenseLineItem> items = report.getLineItems();

        if (items.isEmpty()) {
            violations.add(PolicyViolation.report(RULE_EMPTY,
                    "A report must have at least one line item before it can be submitted."));
            return violations;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate oldestAllowed = today.minusDays(config.getMaxExpenseAgeDays());

        for (ExpenseLineItem item : items) {
            checkReceipt(item, config, violations);
            checkDates(item, today, oldestAllowed, config, violations);
        }

        checkDuplicates(items, violations);
        checkTotalCap(report, config, violations);

        return violations;
    }

    private void checkReceipt(ExpenseLineItem item, PolicyConfig config, List<PolicyViolation> violations) {
        BigDecimal usdAmount = CurrencyConverter.toUsd(item.getAmount(), item.getCurrency(), config.getUsdToInrRate());
        if (usdAmount.compareTo(config.getReceiptThreshold()) > 0 && !item.hasReceipt()) {
            violations.add(PolicyViolation.item(RULE_RECEIPT_REQUIRED,
                    String.format("\"%s\" for %s requires a receipt (amounts over %s must be receipted).",
                            item.getMerchant(), formatMoney(item.getAmount(), item.getCurrency()),
                            formatMoney(config.getReceiptThreshold(), Currency.USD)),
                    item.getId()));
        }
    }

    private void checkDates(ExpenseLineItem item, LocalDate today, LocalDate oldestAllowed,
                            PolicyConfig config, List<PolicyViolation> violations) {
        LocalDate date = item.getDate();
        if (date == null) {
            return;
        }
        if (date.isAfter(today)) {
            violations.add(PolicyViolation.item(RULE_FUTURE_DATE,
                    String.format("\"%s\" is dated in the future (%s).", item.getMerchant(), date),
                    item.getId()));
        } else if (date.isBefore(oldestAllowed)) {
            violations.add(PolicyViolation.item(RULE_TOO_OLD,
                    String.format("\"%s\" (%s) is older than the %d-day submission window.",
                            item.getMerchant(), date, config.getMaxExpenseAgeDays()),
                    item.getId()));
        }
    }

    private void checkDuplicates(List<ExpenseLineItem> items, List<PolicyViolation> violations) {
        Set<String> seen = new HashSet<>();
        for (ExpenseLineItem item : items) {
            String key = item.getDate() + "|"
                    + (item.getAmount() == null ? "" : item.getAmount().stripTrailingZeros().toPlainString()) + "|"
                    + item.getCurrency() + "|"
                    + (item.getMerchant() == null ? "" : item.getMerchant().trim().toLowerCase());
            if (!seen.add(key)) {
                violations.add(PolicyViolation.item(RULE_DUPLICATE,
                        String.format("Duplicate line item: \"%s\" for %s on %s appears more than once.",
                                item.getMerchant(), formatMoney(item.getAmount(), item.getCurrency()), item.getDate()),
                        item.getId()));
            }
        }
    }

    private void checkTotalCap(ExpenseReport report, PolicyConfig config, List<PolicyViolation> violations) {
        report.recomputeTotal(config.getUsdToInrRate());
        BigDecimal total = report.getTotalAmount();
        if (total.compareTo(config.getTotalReportCap()) > 0) {
            violations.add(PolicyViolation.report(RULE_TOTAL_CAP,
                    String.format("Report total %s (USD equivalent) exceeds the cap of %s.",
                            formatMoney(total, Currency.USD), formatMoney(config.getTotalReportCap(), Currency.USD))));
        }
    }

    private String formatMoney(BigDecimal amount, Currency currency) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(2, java.math.RoundingMode.HALF_UP);
        String symbol = currency == Currency.INR ? "₹" : "$";
        return symbol + value.toPlainString();
    }
}
