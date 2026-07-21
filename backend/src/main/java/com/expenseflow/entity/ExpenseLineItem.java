package com.expenseflow.entity;

import com.expenseflow.domain.Currency;
import com.expenseflow.domain.ExpenseCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense_line_items")
public class ExpenseLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private ExpenseReport report;

    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency = Currency.USD;

    @Column(nullable = false)
    private String merchant;

    @Column(length = 500)
    private String notes;

    @OneToOne(mappedBy = "lineItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Receipt receipt;

    public boolean hasReceipt() {
        return receipt != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExpenseReport getReport() { return report; }
    public void setReport(ExpenseReport report) { this.report = report; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Receipt getReceipt() { return receipt; }
    public void setReceipt(Receipt receipt) { this.receipt = receipt; }
}
