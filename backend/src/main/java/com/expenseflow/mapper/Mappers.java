package com.expenseflow.mapper;

import com.expenseflow.dto.ExpenseDtos;
import com.expenseflow.dto.UserDtos;
import com.expenseflow.entity.*;
import com.expenseflow.repository.ReportSummaryView;

/**
 * Central entity -> DTO conversion helpers. Pure functions, no persistence side effects.
 */
public final class Mappers {

    private Mappers() {
    }

    public static UserDtos.UserResponse toUserResponse(User u) {
        User mgr = u.getManager();
        return new UserDtos.UserResponse(
                u.getId(), u.getName(), u.getEmail(), u.getRole(),
                mgr == null ? null : mgr.getId(),
                mgr == null ? null : mgr.getName(),
                u.isActive());
    }

    public static ExpenseDtos.ReceiptResponse toReceiptResponse(Receipt r) {
        if (r == null) {
            return null;
        }
        return new ExpenseDtos.ReceiptResponse(
                r.getId(), r.getFileName(), r.getContentType(), r.getSizeBytes(), r.getUploadedAt());
    }

    public static ExpenseDtos.LineItemResponse toLineItemResponse(ExpenseLineItem li) {
        return new ExpenseDtos.LineItemResponse(
                li.getId(), li.getDate(), li.getCategory(), li.getAmount(), li.getCurrency(),
                li.getMerchant(), li.getNotes(), toReceiptResponse(li.getReceipt()));
    }

    public static ExpenseDtos.ReportResponse toReportResponse(ExpenseReport r) {
        User emp = r.getEmployee();
        User decidedBy = r.getDecidedBy();
        return new ExpenseDtos.ReportResponse(
                r.getId(),
                emp.getId(),
                emp.getName(),
                r.getTitle(),
                r.getPurpose(),
                r.getStatus(),
                r.getTotalAmount(),
                r.getSubmittedAt(),
                r.getDecidedAt(),
                decidedBy == null ? null : decidedBy.getName(),
                r.getRejectionReason(),
                r.getCreatedAt(),
                r.getLineItems().stream()
                        .sorted(java.util.Comparator.comparing(ExpenseLineItem::getId,
                                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                        .map(Mappers::toLineItemResponse)
                        .toList());
    }

    public static ExpenseDtos.ReportSummary toReportSummary(ReportSummaryView v) {
        return new ExpenseDtos.ReportSummary(
                v.getId(),
                v.getEmployeeName(),
                v.getTitle(),
                v.getStatus(),
                v.getTotalAmount(),
                v.getSubmittedAt(),
                v.getCreatedAt(),
                v.getLineItemCount());
    }

    public static ExpenseDtos.AuditEventResponse toAuditResponse(AuditEvent e) {
        return new ExpenseDtos.AuditEventResponse(
                e.getId(), e.getAction(), e.getActor().getName(), e.getNote(), e.getCreatedAt());
    }
}
