package com.expenseflow.service;

import com.expenseflow.domain.AuditAction;
import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.domain.Role;
import com.expenseflow.dto.ExpenseDtos.ReportSummary;
import com.expenseflow.entity.ExpenseReport;
import com.expenseflow.entity.User;
import com.expenseflow.exception.BadRequestException;
import com.expenseflow.exception.ForbiddenException;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.repository.ExpenseReportRepository;
import com.expenseflow.repository.ReportSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ApprovalService {

    private final ExpenseReportRepository reportRepository;
    private final ExpenseReportService reportService;
    private final AuditService auditService;

    public ApprovalService(ExpenseReportRepository reportRepository,
                           ExpenseReportService reportService,
                           AuditService auditService) {
        this.reportRepository = reportRepository;
        this.reportService = reportService;
        this.auditService = auditService;
    }

    /** Reports awaiting this manager's decision (submitted by their direct reports). Admin sees all submitted. */
    @Transactional(readOnly = true)
    public Page<ReportSummary> queue(User me, Pageable pageable) {
        Page<ReportSummaryView> page = me.getRole() == Role.ADMIN
                ? reportRepository.findSummariesByStatus(ExpenseStatus.SUBMITTED, pageable)
                : reportRepository.findSummariesByManagerIdAndStatus(me.getId(), ExpenseStatus.SUBMITTED, pageable);
        return page.map(Mappers::toReportSummary);
    }

    @Transactional
    public ExpenseReport approve(Long reportId, User me) {
        ExpenseReport report = reportService.findById(reportId);
        assertPending(report);
        assertCanDecide(report, me);
        report.setStatus(ExpenseStatus.APPROVED);
        report.setDecidedBy(me);
        report.setDecidedAt(Instant.now());
        auditService.record(report, me, AuditAction.APPROVED, "Approved by " + me.getName());
        return report;
    }

    @Transactional
    public ExpenseReport reject(Long reportId, String reason, User me) {
        ExpenseReport report = reportService.findById(reportId);
        assertPending(report);
        assertCanDecide(report, me);
        report.setStatus(ExpenseStatus.REJECTED);
        report.setDecidedBy(me);
        report.setDecidedAt(Instant.now());
        report.setRejectionReason(reason);
        auditService.record(report, me, AuditAction.REJECTED, reason);
        return report;
    }

    private void assertPending(ExpenseReport report) {
        if (report.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new BadRequestException("Only a submitted report can be approved or rejected (status: "
                    + report.getStatus() + ").");
        }
    }

    private void assertCanDecide(ExpenseReport report, User me) {
        if (me.getRole() == Role.ADMIN) {
            return;
        }
        boolean isTheirManager = me.getRole() == Role.MANAGER
                && report.getEmployee().getManager() != null
                && report.getEmployee().getManager().getId().equals(me.getId());
        if (!isTheirManager) {
            throw new ForbiddenException("Only the employee's manager may decide on this report.");
        }
    }
}
