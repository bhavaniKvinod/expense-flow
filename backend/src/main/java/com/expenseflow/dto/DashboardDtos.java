package com.expenseflow.dto;

public class DashboardDtos {

    public record DashboardResponse(
            long myDrafts,
            long mySubmitted,
            long myApproved,
            long myRejected,
            long myReimbursed,
            // Manager/Admin only (0 for employees):
            long awaitingMyApproval,
            // Admin only (0 otherwise):
            long awaitingReimbursement) {
    }
}
