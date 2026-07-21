package com.expenseflow.controller;

import com.expenseflow.dto.ExpenseDtos;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.security.CurrentUserProvider;
import com.expenseflow.service.ApprovalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final CurrentUserProvider currentUser;

    public ApprovalController(ApprovalService approvalService, CurrentUserProvider currentUser) {
        this.approvalService = approvalService;
        this.currentUser = currentUser;
    }

    @GetMapping("/approvals")
    public List<ExpenseDtos.ReportSummary> queue() {
        return approvalService.queue(currentUser.require()).stream()
                .map(Mappers::toReportSummary).toList();
    }

    @PostMapping("/reports/{id}/approve")
    public ExpenseDtos.ReportResponse approve(@PathVariable Long id) {
        return Mappers.toReportResponse(approvalService.approve(id, currentUser.require()));
    }

    @PostMapping("/reports/{id}/reject")
    public ExpenseDtos.ReportResponse reject(@PathVariable Long id,
                                             @Valid @RequestBody ExpenseDtos.RejectRequest request) {
        return Mappers.toReportResponse(approvalService.reject(id, request.reason(), currentUser.require()));
    }
}
