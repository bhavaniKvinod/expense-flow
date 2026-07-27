package com.expenseflow.service;

import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.domain.Role;
import com.expenseflow.dto.DashboardDtos;
import com.expenseflow.entity.User;
import com.expenseflow.repository.ExpenseReportRepository;
import com.expenseflow.repository.StatusCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ExpenseReportRepository reportRepository;

    public DashboardService(ExpenseReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardResponse forUser(User me) {
        Long id = me.getId();
        long awaitingApproval = 0;
        long awaitingReimbursement = 0;

        if (me.getRole() == Role.ADMIN) {
            awaitingApproval = reportRepository.countByStatus(ExpenseStatus.SUBMITTED);
            awaitingReimbursement = reportRepository.countByStatus(ExpenseStatus.APPROVED);
        } else if (me.getRole() == Role.MANAGER) {
            awaitingApproval = reportRepository.countByEmployeeManagerIdAndStatus(id, ExpenseStatus.SUBMITTED);
        }

        // Single GROUP BY query instead of one countByEmployeeIdAndStatus round trip per status.
        Map<ExpenseStatus, Long> mine = reportRepository.countGroupedByStatusForEmployee(id).stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCount));

        return new DashboardDtos.DashboardResponse(
                mine.getOrDefault(ExpenseStatus.DRAFT, 0L),
                mine.getOrDefault(ExpenseStatus.SUBMITTED, 0L),
                mine.getOrDefault(ExpenseStatus.APPROVED, 0L),
                mine.getOrDefault(ExpenseStatus.REJECTED, 0L),
                mine.getOrDefault(ExpenseStatus.REIMBURSED, 0L),
                awaitingApproval,
                awaitingReimbursement);
    }
}
