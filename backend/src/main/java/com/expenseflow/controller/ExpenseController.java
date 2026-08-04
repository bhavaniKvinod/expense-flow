package com.expenseflow.controller;

import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.dto.ExpenseDtos;
import com.expenseflow.dto.PageResponse;
import com.expenseflow.entity.ExpenseReport;
import com.expenseflow.entity.User;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.policy.PolicyViolation;
import com.expenseflow.security.CurrentUserProvider;
import com.expenseflow.service.AuditService;
import com.expenseflow.service.ExpenseReportService;
import com.expenseflow.web.PagingSupport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ExpenseController {

    private final ExpenseReportService reportService;
    private final AuditService auditService;
    private final CurrentUserProvider currentUser;

    public ExpenseController(ExpenseReportService reportService,
                            AuditService auditService,
                            CurrentUserProvider currentUser) {
        this.reportService = reportService;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    // ---- Reports ----

    @GetMapping("/api/reports")
    public PageResponse<ExpenseDtos.ReportSummary> myReports(@RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size,
                                                             @RequestParam(required = false) ExpenseStatus status) {
        User me = currentUser.require();
        return PageResponse.from(reportService.listMine(me, status, PagingSupport.of(page, size)));
    }

    @PostMapping("/api/reports")
    public ExpenseDtos.ReportResponse create(@Valid @RequestBody ExpenseDtos.CreateReportRequest request) {
        User me = currentUser.require();
        return Mappers.toReportResponse(reportService.create(request, me));
    }

    @GetMapping("/api/reports/{id}")
    public ExpenseDtos.ReportResponse get(@PathVariable Long id) {
        User me = currentUser.require();
        return Mappers.toReportResponse(reportService.getForViewing(id, me));
    }

    @PutMapping("/api/reports/{id}")
    public ExpenseDtos.ReportResponse update(@PathVariable Long id,
                                             @Valid @RequestBody ExpenseDtos.UpdateReportRequest request) {
        User me = currentUser.require();
        return Mappers.toReportResponse(reportService.update(id, request, me));
    }

    @DeleteMapping("/api/reports/{id}")
    public void delete(@PathVariable Long id) {
        reportService.delete(id, currentUser.require());
    }

    @PostMapping("/api/reports/{id}/submit")
    public ExpenseDtos.ReportResponse submit(@PathVariable Long id) {
        return Mappers.toReportResponse(reportService.submit(id, currentUser.require()));
    }

    @PostMapping("/api/reports/{id}/cancel")
    public ExpenseDtos.ReportResponse cancel(@PathVariable Long id) {
        return Mappers.toReportResponse(reportService.cancel(id, currentUser.require()));
    }

    @PostMapping("/api/reports/{id}/check")
    public ExpenseDtos.PolicyCheckResponse check(@PathVariable Long id) {
        List<PolicyViolation> violations = reportService.dryRunCheck(id, currentUser.require());
        return new ExpenseDtos.PolicyCheckResponse(violations.isEmpty(), violations);
    }

    @GetMapping("/api/reports/{id}/audit")
    public List<ExpenseDtos.AuditEventResponse> audit(@PathVariable Long id) {
        User me = currentUser.require();
        ExpenseReport report = reportService.getForViewing(id, me); // enforces view permission
        return auditService.forReport(report.getId()).stream().map(Mappers::toAuditResponse).toList();
    }

    // ---- Line items ----

    @PostMapping("/api/reports/{id}/line-items")
    public ExpenseDtos.ReportResponse addLineItem(@PathVariable Long id,
                                                  @Valid @RequestBody ExpenseDtos.LineItemRequest request) {
        User me = currentUser.require();
        reportService.addLineItem(id, request, me);
        return Mappers.toReportResponse(reportService.getForViewing(id, me));
    }

    @PutMapping("/api/line-items/{lineItemId}")
    public ExpenseDtos.LineItemResponse updateLineItem(@PathVariable Long lineItemId,
                                                       @Valid @RequestBody ExpenseDtos.LineItemRequest request) {
        return Mappers.toLineItemResponse(
                reportService.updateLineItem(lineItemId, request, currentUser.require()));
    }

    @DeleteMapping("/api/line-items/{lineItemId}")
    public void deleteLineItem(@PathVariable Long lineItemId) {
        reportService.deleteLineItem(lineItemId, currentUser.require());
    }

    /** Mock receipt upload — stores file metadata only, never the bytes. */
    @PostMapping("/api/line-items/{lineItemId}/receipt")
    public ExpenseDtos.LineItemResponse uploadReceipt(@PathVariable Long lineItemId,
                                                      @RequestParam("file") MultipartFile file) {
        User me = currentUser.require();
        return Mappers.toLineItemResponse(reportService.attachReceipt(
                lineItemId,
                file.getOriginalFilename() == null ? "receipt" : file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                me));
    }
}
