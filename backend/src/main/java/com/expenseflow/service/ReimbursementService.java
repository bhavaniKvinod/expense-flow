package com.expenseflow.service;

import com.expenseflow.domain.AuditAction;
import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.dto.ExpenseDtos.ReportSummary;
import com.expenseflow.entity.ExpenseReport;
import com.expenseflow.entity.Reimbursement;
import com.expenseflow.entity.User;
import com.expenseflow.exception.BadRequestException;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.repository.ExpenseReportRepository;
import com.expenseflow.repository.ReimbursementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReimbursementService {

    private final ExpenseReportRepository reportRepository;
    private final ReimbursementRepository reimbursementRepository;
    private final ExpenseReportService reportService;
    private final AuditService auditService;

    public ReimbursementService(ExpenseReportRepository reportRepository,
                                ReimbursementRepository reimbursementRepository,
                                ExpenseReportService reportService,
                                AuditService auditService) {
        this.reportRepository = reportRepository;
        this.reimbursementRepository = reimbursementRepository;
        this.reportService = reportService;
        this.auditService = auditService;
    }

    /** Approved reports awaiting payment. */
    @Transactional(readOnly = true)
    public Page<ReportSummary> queue(Pageable pageable) {
        return reportRepository.findSummariesByStatus(ExpenseStatus.APPROVED, pageable).map(Mappers::toReportSummary);
    }

    /**
     * Mock payment — records a reimbursement row and moves the report to REIMBURSED.
     * No real money movement; this is the "actual payment" external touchpoint, mocked.
     */
    @Transactional
    public ExpenseReport reimburse(Long reportId, String paymentReference, User me) {
        ExpenseReport report = reportService.findById(reportId);
        if (report.getStatus() != ExpenseStatus.APPROVED) {
            throw new BadRequestException("Only an approved report can be reimbursed (status: "
                    + report.getStatus() + ").");
        }

        String reference = (paymentReference == null || paymentReference.isBlank())
                ? "PAY-" + report.getId() + "-" + Instant.now().toEpochMilli()
                : paymentReference.trim();

        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setReport(report);
        reimbursement.setPaymentReference(reference);
        reimbursement.setPaidBy(me);
        reimbursement.setPaidAt(Instant.now());
        reimbursementRepository.save(reimbursement);

        report.setStatus(ExpenseStatus.REIMBURSED);
        auditService.record(report, me, AuditAction.REIMBURSED, "Reimbursed (ref " + reference + ")");
        return report;
    }
}
