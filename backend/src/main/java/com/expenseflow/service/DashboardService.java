package com.expenseflow.service;

import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.domain.Role;
import com.expenseflow.dto.DashboardDtos;
import com.expenseflow.entity.User;
import com.expenseflow.repository.ExpenseReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return new DashboardDtos.DashboardResponse(
                reportRepository.countByEmployeeIdAndStatus(id, ExpenseStatus.DRAFT),
                reportRepository.countByEmployeeIdAndStatus(id, ExpenseStatus.SUBMITTED),
                reportRepository.countByEmployeeIdAndStatus(id, ExpenseStatus.APPROVED),
                reportRepository.countByEmployeeIdAndStatus(id, ExpenseStatus.REJECTED),
                reportRepository.countByEmployeeIdAndStatus(id, ExpenseStatus.REIMBURSED),
                awaitingApproval,
                awaitingReimbursement);
    }
}
