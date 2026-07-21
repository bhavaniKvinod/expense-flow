package com.expenseflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Singleton configuration row (id = 1) holding the tunable policy thresholds.
 */
@Entity
@Table(name = "policy_config")
public class PolicyConfig {

    @Id
    private Long id = 1L;

    /** Line items above this amount require a receipt. */
    @Column(name = "receipt_threshold", nullable = false, precision = 12, scale = 2)
    private BigDecimal receiptThreshold;

    /** A report whose total exceeds this cap is rejected at submission. */
    @Column(name = "total_report_cap", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalReportCap;

    /** Expenses older than this many days are rejected. */
    @Column(name = "max_expense_age_days", nullable = false)
    private int maxExpenseAgeDays;

    /**
     * Fixed conversion rate used to normalise non-USD line items (INR) to USD for
     * report totals and policy checks. E.g. 83.0000 means 1 USD = 83 INR.
     */
    @Column(name = "usd_to_inr_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal usdToInrRate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getReceiptThreshold() { return receiptThreshold; }
    public void setReceiptThreshold(BigDecimal receiptThreshold) { this.receiptThreshold = receiptThreshold; }

    public BigDecimal getTotalReportCap() { return totalReportCap; }
    public void setTotalReportCap(BigDecimal totalReportCap) { this.totalReportCap = totalReportCap; }

    public int getMaxExpenseAgeDays() { return maxExpenseAgeDays; }
    public void setMaxExpenseAgeDays(int maxExpenseAgeDays) { this.maxExpenseAgeDays = maxExpenseAgeDays; }

    public BigDecimal getUsdToInrRate() { return usdToInrRate; }
    public void setUsdToInrRate(BigDecimal usdToInrRate) { this.usdToInrRate = usdToInrRate; }
}
