package com.expenseflow.policy;

import com.expenseflow.domain.Currency;
import com.expenseflow.domain.ExpenseCategory;
import com.expenseflow.entity.ExpenseLineItem;
import com.expenseflow.entity.ExpenseReport;
import com.expenseflow.entity.PolicyConfig;
import com.expenseflow.entity.Receipt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);

    private final Clock clock = Clock.fixed(
            TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
    private final PolicyEngine engine = new PolicyEngine(clock);

    private PolicyConfig config() {
        PolicyConfig c = new PolicyConfig();
        c.setReceiptThreshold(new BigDecimal("25.00"));
        c.setTotalReportCap(new BigDecimal("5000.00"));
        c.setMaxExpenseAgeDays(90);
        c.setUsdToInrRate(new BigDecimal("83.0000"));
        c.setUsdToEurRate(new BigDecimal("0.9200"));
        c.setUsdToAedRate(new BigDecimal("3.6725"));
        return c;
    }

    private ExpenseLineItem item(LocalDate date, String amount, String merchant, boolean receipt) {
        return item(date, amount, Currency.USD, merchant, receipt);
    }

    private ExpenseLineItem item(LocalDate date, String amount, Currency currency, String merchant, boolean receipt) {
        ExpenseLineItem li = new ExpenseLineItem();
        li.setDate(date);
        li.setCategory(ExpenseCategory.MEALS);
        li.setAmount(new BigDecimal(amount));
        li.setCurrency(currency);
        li.setMerchant(merchant);
        if (receipt) {
            Receipt r = new Receipt();
            r.setFileName("receipt.pdf");
            r.setLineItem(li);
            li.setReceipt(r);
        }
        return li;
    }

    private ExpenseReport reportWith(ExpenseLineItem... items) {
        ExpenseReport report = new ExpenseReport();
        for (ExpenseLineItem li : items) {
            li.setReport(report);
            report.getLineItems().add(li);
        }
        return report;
    }

    @Test
    void validReportHasNoViolations() {
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(2), "18.00", "Coffee Shop", false),
                item(TODAY.minusDays(1), "120.00", "Hotel", true));

        assertThat(engine.validate(report, config())).isEmpty();
    }

    @Test
    void emptyReportIsRejected() {
        List<PolicyViolation> violations = engine.validate(reportWith(), config());
        assertThat(violations).extracting(PolicyViolation::rule).containsExactly(PolicyEngine.RULE_EMPTY);
    }

    @Test
    void itemOverThresholdWithoutReceiptIsRejected() {
        ExpenseReport report = reportWith(item(TODAY.minusDays(1), "40.00", "Restaurant", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_RECEIPT_REQUIRED);
    }

    @Test
    void itemAtOrBelowThresholdNeedsNoReceipt() {
        ExpenseReport report = reportWith(item(TODAY.minusDays(1), "25.00", "Restaurant", false));
        assertThat(engine.validate(report, config())).isEmpty();
    }

    @Test
    void totalOverCapIsRejected() {
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "3000.00", "Vendor A", true),
                item(TODAY.minusDays(1), "2500.00", "Vendor B", true));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_TOTAL_CAP);
    }

    @Test
    void futureDatedItemIsRejected() {
        ExpenseReport report = reportWith(item(TODAY.plusDays(1), "10.00", "Cafe", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_FUTURE_DATE);
    }

    @Test
    void tooOldItemIsRejected() {
        ExpenseReport report = reportWith(item(TODAY.minusDays(120), "10.00", "Cafe", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_TOO_OLD);
    }

    @Test
    void duplicateLineItemsAreRejected() {
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(3), "12.00", "Taxi", false),
                item(TODAY.minusDays(3), "12.00", "taxi", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_DUPLICATE);
    }

    @Test
    void reportsAllViolationsAtOnce() {
        ExpenseReport report = reportWith(
                item(TODAY.plusDays(1), "40.00", "Future Vendor", false)); // future + missing receipt
        List<PolicyViolation> violations = engine.validate(report, config());
        assertThat(violations).extracting(PolicyViolation::rule)
                .contains(PolicyEngine.RULE_FUTURE_DATE, PolicyEngine.RULE_RECEIPT_REQUIRED);
    }

    @Test
    void inrItemIsConvertedToUsdForReceiptCheck() {
        // 83 INR == 1 USD; 3000 INR ~= $36.14, over the $25 threshold -> receipt required.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "3000.00", Currency.INR, "Mumbai Cafe", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_RECEIPT_REQUIRED);
    }

    @Test
    void inrItemBelowUsdEquivalentThresholdNeedsNoReceipt() {
        // 2000 INR ~= $24.10, under the $25 threshold.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "2000.00", Currency.INR, "Mumbai Cafe", false));
        assertThat(engine.validate(report, config())).isEmpty();
    }

    @Test
    void mixedCurrencyTotalIsConvertedToUsdForCapCheck() {
        // $3000 USD + 166100 INR (~= $2001.20 USD) => ~$5001.20 total, over the $5000 cap.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "3000.00", Currency.USD, "Vendor A", true),
                item(TODAY.minusDays(1), "166100.00", Currency.INR, "Vendor B", true));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_TOTAL_CAP);
    }

    @Test
    void eurItemIsConvertedToUsdForReceiptCheck() {
        // 0.92 EUR == 1 USD; 30 EUR ~= $32.61, over the $25 threshold -> receipt required.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "30.00", Currency.EUR, "Paris Cafe", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_RECEIPT_REQUIRED);
    }

    @Test
    void eurItemBelowUsdEquivalentThresholdNeedsNoReceipt() {
        // 20 EUR ~= $21.74, under the $25 threshold.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "20.00", Currency.EUR, "Paris Cafe", false));
        assertThat(engine.validate(report, config())).isEmpty();
    }

    @Test
    void mixedEurCurrencyTotalIsConvertedToUsdForCapCheck() {
        // $3000 USD + 1840 EUR (~= $2000 USD) => ~$5000 total, at the cap; push over with one more euro.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "3000.00", Currency.USD, "Vendor A", true),
                item(TODAY.minusDays(1), "1841.00", Currency.EUR, "Vendor B", true));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_TOTAL_CAP);
    }

    @Test
    void aedItemIsConvertedToUsdForReceiptCheck() {
        // 3.6725 AED == 1 USD; 100 AED ~= $27.23, over the $25 threshold -> receipt required.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "100.00", Currency.AED, "Dubai Cafe", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_RECEIPT_REQUIRED);
    }

    @Test
    void aedItemBelowUsdEquivalentThresholdNeedsNoReceipt() {
        // 80 AED ~= $21.78, under the $25 threshold.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "80.00", Currency.AED, "Dubai Cafe", false));
        assertThat(engine.validate(report, config())).isEmpty();
    }

    @Test
    void mixedAedCurrencyTotalIsConvertedToUsdForCapCheck() {
        // $3000 USD + 7346 AED (~= $2000.27 USD) => ~$5000.27 total, over the $5000 cap.
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "3000.00", Currency.USD, "Vendor A", true),
                item(TODAY.minusDays(1), "7346.00", Currency.AED, "Vendor B", true));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).contains(PolicyEngine.RULE_TOTAL_CAP);
    }

    @Test
    void sameAmountDifferentCurrencyIsNotADuplicate() {
        ExpenseReport report = reportWith(
                item(TODAY.minusDays(1), "12.00", Currency.USD, "Taxi", false),
                item(TODAY.minusDays(1), "12.00", Currency.INR, "Taxi", false));
        assertThat(engine.validate(report, config()))
                .extracting(PolicyViolation::rule).doesNotContain(PolicyEngine.RULE_DUPLICATE);
    }
}
