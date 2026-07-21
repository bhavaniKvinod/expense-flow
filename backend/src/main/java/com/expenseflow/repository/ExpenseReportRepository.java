package com.expenseflow.repository;

import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.entity.ExpenseReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long> {

    List<ExpenseReport> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<ExpenseReport> findByStatusOrderBySubmittedAtAsc(ExpenseStatus status);

    List<ExpenseReport> findByEmployeeManagerIdAndStatusOrderBySubmittedAtAsc(Long managerId, ExpenseStatus status);

    long countByEmployeeIdAndStatus(Long employeeId, ExpenseStatus status);

    long countByStatus(ExpenseStatus status);

    long countByEmployeeManagerIdAndStatus(Long managerId, ExpenseStatus status);
}
