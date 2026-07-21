package com.expenseflow.controller;

import com.expenseflow.dto.PolicyDtos;
import com.expenseflow.entity.PolicyConfig;
import com.expenseflow.service.PolicyConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    private final PolicyConfigService policyConfigService;

    public PolicyController(PolicyConfigService policyConfigService) {
        this.policyConfigService = policyConfigService;
    }

    /** Readable by any authenticated user so employees can see the rules before submitting. */
    @GetMapping
    public PolicyDtos.PolicyResponse get() {
        return toResponse(policyConfigService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PolicyDtos.PolicyResponse update(@Valid @RequestBody PolicyDtos.UpdatePolicyRequest request) {
        return toResponse(policyConfigService.update(request));
    }

    private PolicyDtos.PolicyResponse toResponse(PolicyConfig c) {
        return new PolicyDtos.PolicyResponse(
                c.getReceiptThreshold(), c.getTotalReportCap(), c.getMaxExpenseAgeDays(), c.getUsdToInrRate());
    }
}
