package com.expenseflow.controller;

import com.expenseflow.dto.ExpenseDtos;
import com.expenseflow.dto.PageResponse;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.security.CurrentUserProvider;
import com.expenseflow.service.ReimbursementService;
import com.expenseflow.web.PagingSupport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<ExpenseDtos.ReportSummary> queue(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return PageResponse.from(reimbursementService.queue(PagingSupport.of(page, size)));
    }

    @PostMapping("/reports/{id}/reimburse")
    public ExpenseDtos.ReportResponse reimburse(@PathVariable Long id,
                                                @RequestBody(required = false) ExpenseDtos.ReimburseRequest request) {
        String reference = request == null ? null : request.paymentReference();
        return Mappers.toReportResponse(
                reimbursementService.reimburse(id, reference, currentUser.require()));
    }
}
