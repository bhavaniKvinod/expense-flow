package com.expenseflow.dto;

import com.expenseflow.domain.AuditAction;
import com.expenseflow.domain.Currency;
import com.expenseflow.domain.ExpenseCategory;
import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.policy.PolicyViolation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ExpenseDtos {

    // ---- Requests ----

    public record CreateReportRequest(
            @NotBlank String title,
            String purpose) {
    }

    public record UpdateReportRequest(
            @NotBlank String title,
            String purpose) {
    }

    public record LineItemRequest(
            @NotNull LocalDate date,
            @NotNull ExpenseCategory category,
            @NotNull @Positive @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull Currency currency,
            @NotBlank String merchant,
            String notes) {
    }

    public record RejectRequest(
            @NotBlank(message = "A rejection reason is required") String reason) {
    }

    public record ReimburseRequest(
            String paymentReference) {
    }

    // ---- Responses ----

    public record ReceiptResponse(
            Long id, String fileName, String contentType, Long sizeBytes, Instant uploadedAt) {
    }

    public record LineItemResponse(
            Long id, LocalDate date, ExpenseCategory category, BigDecimal amount, Currency currency,
            String merchant, String notes, ReceiptResponse receipt) {
    }

    public record ReportResponse(
            Long id,
            Long employeeId,
            String employeeName,
            String title,
            String purpose,
            ExpenseStatus status,
            BigDecimal totalAmount,
            Instant submittedAt,
            Instant decidedAt,
            String decidedByName,
            String rejectionReason,
            Instant createdAt,
            List<LineItemResponse> lineItems) {
    }

    public record ReportSummary(
            Long id,
            String employeeName,
            String title,
            ExpenseStatus status,
            BigDecimal totalAmount,
            Instant submittedAt,
            Instant createdAt,
            int lineItemCount) {
    }

    public record AuditEventResponse(
            Long id, AuditAction action, String actorName, String note, Instant createdAt) {
    }

    public record PolicyCheckResponse(
            boolean passed,
            List<PolicyViolation> violations) {
    }
}
