package com.expenseflow.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Mock payment record — represents the "actual payment" external touchpoint that is intentionally mocked.
 */
@Entity
@Table(name = "reimbursements")
public class Reimbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private ExpenseReport report;

    @Column(name = "payment_reference", nullable = false)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExpenseReport getReport() { return report; }
    public void setReport(ExpenseReport report) { this.report = report; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public User getPaidBy() { return paidBy; }
    public void setPaidBy(User paidBy) { this.paidBy = paidBy; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
