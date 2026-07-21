package com.expenseflow.repository;

import com.expenseflow.entity.ExpenseLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseLineItemRepository extends JpaRepository<ExpenseLineItem, Long> {
}
