package com.expenseflow.repository;

import com.expenseflow.domain.ExpenseStatus;

/** Interface projection for the {@code GROUP BY status} dashboard aggregation query. */
public interface StatusCount {
    ExpenseStatus getStatus();
    long getCount();
}
