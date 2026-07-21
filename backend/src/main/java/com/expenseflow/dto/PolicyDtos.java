package com.expenseflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class PolicyDtos {

    public record PolicyResponse(
            BigDecimal receiptThreshold,
            BigDecimal totalReportCap,
            int maxExpenseAgeDays,
            BigDecimal usdToInrRate) {
    }

    public record UpdatePolicyRequest(
            @NotNull @PositiveOrZero BigDecimal receiptThreshold,
            @NotNull @PositiveOrZero BigDecimal totalReportCap,
            @Min(1) int maxExpenseAgeDays,
            @NotNull @PositiveOrZero BigDecimal usdToInrRate) {
    }
}
