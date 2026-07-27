package com.expenseflow.service;

import com.expenseflow.domain.AuditAction;
import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.domain.Role;
import com.expenseflow.dto.ExpenseDtos;
import com.expenseflow.entity.*;
import com.expenseflow.exception.BadRequestException;
import com.expenseflow.exception.ForbiddenException;
import com.expenseflow.exception.NotFoundException;
import com.expenseflow.exception.PolicyViolationException;
import com.expenseflow.policy.PolicyEngine;
import com.expenseflow.policy.PolicyViolation;
import com.expenseflow.dto.ExpenseDtos.ReportSummary;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.repository.ExpenseLineItemRepository;
import com.expenseflow.repository.ExpenseReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ExpenseReportService {

    private final ExpenseReportRepository reportRepository;
    private final ExpenseLineItemRepository lineItemRepository;
    private final PolicyEngine policyEngine;
    private final PolicyConfigService policyConfigService;
    private final AuditService auditService;

    public ExpenseReportService(ExpenseReportRepository reportRepository,
                                ExpenseLineItemRepository lineItemRepository,
                                PolicyEngine policyEngine,
                                PolicyConfigService policyConfigService,
                                AuditService auditService) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.policyEngine = policyEngine;
        this.policyConfigService = policyConfigService;
        this.auditService = auditService;
    }

    // ---- Reads ----

    @Transactional(readOnly = true)
    public Page<ReportSummary> listMine(User me, Pageable pageable) {
        return reportRepository.findSummariesByEmployeeId(me.getId(), pageable).map(Mappers::toReportSummary);
    }

    @Transactional(readOnly = true)
    public ExpenseReport getForViewing(Long reportId, User me) {
        ExpenseReport report = findWithDetails(reportId);
        assertCanView(report, me);
        return report;
    }

    // ---- Report lifecycle ----

    @Transactional
    public ExpenseReport create(ExpenseDtos.CreateReportRequest request, User me) {
        ExpenseReport report = new ExpenseReport();
        report.setEmployee(me);
        report.setTitle(request.title());
        report.setPurpose(request.purpose());
        report.setStatus(ExpenseStatus.DRAFT);
        report.setCreatedAt(Instant.now());
        ExpenseReport saved = reportRepository.save(report);
        auditService.record(saved, me, AuditAction.CREATED, "Report created");
        return saved;
    }

    @Transactional
    public ExpenseReport update(Long reportId, ExpenseDtos.UpdateReportRequest request, User me) {
        ExpenseReport report = findById(reportId);
        assertOwner(report, me);
        assertEditable(report);
        report.setTitle(request.title());
        report.setPurpose(request.purpose());
        return report;
    }

    @Transactional
    public void delete(Long reportId, User me) {
        ExpenseReport report = findById(reportId);
        assertOwner(report, me);
        if (report.getStatus() != ExpenseStatus.DRAFT) {
            throw new BadRequestException("Only draft reports can be deleted.");
        }
        reportRepository.delete(report);
    }

    @Transactional
    public ExpenseReport submit(Long reportId, User me) {
        ExpenseReport report = findById(reportId);
        assertOwner(report, me);
        boolean resubmit = report.getStatus() == ExpenseStatus.REJECTED;
        if (report.getStatus() != ExpenseStatus.DRAFT && !resubmit) {
            throw new BadRequestException("Only draft or rejected reports can be submitted.");
        }

        recomputeTotal(report);
        List<PolicyViolation> violations = policyEngine.validate(report, policyConfigService.get());
        if (!violations.isEmpty()) {
            throw new PolicyViolationException(violations);
        }

        report.setStatus(ExpenseStatus.SUBMITTED);
        report.setSubmittedAt(Instant.now());
        report.setRejectionReason(null);
        report.setDecidedAt(null);
        report.setDecidedBy(null);
        auditService.record(report, me,
                resubmit ? AuditAction.RESUBMITTED : AuditAction.SUBMITTED,
                "Submitted for approval");
        return report;
    }

    @Transactional
    public ExpenseReport cancel(Long reportId, User me) {
        ExpenseReport report = findById(reportId);
        assertOwner(report, me);
        if (report.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new BadRequestException("Only a submitted report awaiting a decision can be cancelled.");
        }
        report.setStatus(ExpenseStatus.CANCELLED);
        auditService.record(report, me, AuditAction.CANCELLED, "Withdrawn by employee");
        return report;
    }

    /** Non-mutating policy evaluation for the live "check" endpoint. */
    @Transactional(readOnly = true)
    public List<PolicyViolation> dryRunCheck(Long reportId, User me) {
        ExpenseReport report = findById(reportId);
        assertOwner(report, me);
        recomputeTotal(report);
        return policyEngine.validate(report, policyConfigService.get());
    }

    // ---- Line items ----

    @Transactional
    public ExpenseLineItem addLineItem(Long reportId, ExpenseDtos.LineItemRequest request, User me) {
        ExpenseReport report = findById(reportId);
        assertOwner(report, me);
        assertEditable(report);

        ExpenseLineItem item = new ExpenseLineItem();
        item.setReport(report);
        applyLineItem(item, request);
        report.getLineItems().add(item);
        lineItemRepository.save(item);
        recomputeTotal(report);
        return item;
    }

    @Transactional
    public ExpenseLineItem updateLineItem(Long lineItemId, ExpenseDtos.LineItemRequest request, User me) {
        ExpenseLineItem item = findLineItem(lineItemId);
        ExpenseReport report = item.getReport();
        assertOwner(report, me);
        assertEditable(report);
        applyLineItem(item, request);
        recomputeTotal(report);
        return item;
    }

    @Transactional
    public void deleteLineItem(Long lineItemId, User me) {
        ExpenseLineItem item = findLineItem(lineItemId);
        ExpenseReport report = item.getReport();
        assertOwner(report, me);
        assertEditable(report);
        report.getLineItems().remove(item);
        lineItemRepository.delete(item);
        recomputeTotal(report);
    }

    /** Mock receipt attach — stores metadata only, replaces any existing receipt on the line item. */
    @Transactional
    public ExpenseLineItem attachReceipt(Long lineItemId, String fileName, String contentType,
                                         Long sizeBytes, User me) {
        ExpenseLineItem item = findLineItem(lineItemId);
        ExpenseReport report = item.getReport();
        assertOwner(report, me);
        assertEditable(report);

        Receipt receipt = new Receipt();
        receipt.setLineItem(item);
        receipt.setFileName(fileName);
        receipt.setContentType(contentType);
        receipt.setSizeBytes(sizeBytes);
        receipt.setUploadedAt(Instant.now());
        item.setReceipt(receipt);
        return item;
    }

    // ---- Helpers ----

    private void applyLineItem(ExpenseLineItem item, ExpenseDtos.LineItemRequest request) {
        item.setDate(request.date());
        item.setCategory(request.category());
        item.setAmount(request.amount());
        item.setCurrency(request.currency());
        item.setMerchant(request.merchant());
        item.setNotes(request.notes());
    }

    private void recomputeTotal(ExpenseReport report) {
        report.recomputeTotal(policyConfigService.get().getUsdToInrRate());
    }

    public ExpenseReport findById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report " + reportId + " not found"));
    }

    private ExpenseReport findWithDetails(Long reportId) {
        return reportRepository.findWithDetailsById(reportId)
                .orElseThrow(() -> new NotFoundException("Report " + reportId + " not found"));
    }

    private ExpenseLineItem findLineItem(Long lineItemId) {
        return lineItemRepository.findById(lineItemId)
                .orElseThrow(() -> new NotFoundException("Line item " + lineItemId + " not found"));
    }

    private void assertOwner(ExpenseReport report, User me) {
        if (!report.getEmployee().getId().equals(me.getId())) {
            throw new ForbiddenException("You can only modify your own reports.");
        }
    }

    private void assertEditable(ExpenseReport report) {
        if (report.getStatus() != ExpenseStatus.DRAFT && report.getStatus() != ExpenseStatus.REJECTED) {
            throw new BadRequestException("This report can no longer be edited (status: "
                    + report.getStatus() + ").");
        }
    }

    private void assertCanView(ExpenseReport report, User me) {
        if (me.getRole() == Role.ADMIN) {
            return;
        }
        if (report.getEmployee().getId().equals(me.getId())) {
            return;
        }
        boolean isTheirManager = me.getRole() == Role.MANAGER
                && report.getEmployee().getManager() != null
                && report.getEmployee().getManager().getId().equals(me.getId());
        if (!isTheirManager) {
            throw new ForbiddenException("You are not allowed to view this report.");
        }
    }
}
