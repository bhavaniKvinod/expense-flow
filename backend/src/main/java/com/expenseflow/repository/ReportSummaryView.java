package com.expenseflow.repository;

import com.expenseflow.domain.ExpenseStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Interface projection backing the list/queue queries in {@link ExpenseReportRepository}.
 * Selected directly from JPQL (including a {@code SIZE(r.lineItems)} subquery for the item
 * count) so list endpoints never touch the lazy {@code employee}/{@code lineItems}
 * associations on {@code ExpenseReport}, avoiding the N+1 that {@code Mappers.toReportSummary}
 * used to trigger per row.
 */
public interface ReportSummaryView {
    Long getId();
    String getEmployeeName();
    String getTitle();
    ExpenseStatus getStatus();
    BigDecimal getTotalAmount();
    Instant getSubmittedAt();
    Instant getCreatedAt();
    Integer getLineItemCount();
}
