package com.expenseflow.controller;

import com.expenseflow.dto.ExpenseDtos;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.security.CurrentUserProvider;
import com.expenseflow.service.ReimbursementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class ReimbursementController {

    private final ReimbursementService reimbursementService;
    private final CurrentUserProvider currentUser;

    public ReimbursementController(ReimbursementService reimbursementService, CurrentUserProvider currentUser) {
        this.reimbursementService = reimbursementService;
        this.currentUser = currentUser;
    }

    @GetMapping("/reimbursements")
    public List<ExpenseDtos.ReportSummary> queue() {
        return reimbursementService.queue().stream().map(Mappers::toReportSummary).toList();
    }

    @PostMapping("/reports/{id}/reimburse")
    public ExpenseDtos.ReportResponse reimburse(@PathVariable Long id,
                                                @RequestBody(required = false) ExpenseDtos.ReimburseRequest request) {
        String reference = request == null ? null : request.paymentReference();
        return Mappers.toReportResponse(
                reimbursementService.reimburse(id, reference, currentUser.require()));
    }
}
