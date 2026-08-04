package com.expenseflow.repository;

import com.expenseflow.domain.ExpenseStatus;
import com.expenseflow.entity.ExpenseReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, Long> {

    /**
     * Single-report fetch used by the "view a report" paths, eagerly loading every
     * association {@code Mappers.toReportResponse} walks (employee, decidedBy, line items
     * and each line item's receipt) in one query instead of resolving them lazily one at a time.
     */
    @EntityGraph(attributePaths = {"employee", "decidedBy", "lineItems", "lineItems.receipt"})
    Optional<ExpenseReport> findWithDetailsById(Long id);

    /**
     * List/queue summaries are projected directly via JPQL (including a {@code SIZE(...)}
     * subquery for the line item count) so paging a report list never touches the lazy
     * {@code employee}/{@code lineItems} associations per row - eliminating the N+1 that
     * {@code Mappers.toReportSummary(ExpenseReport)} used to cause.
     */
    @Query("""
            SELECT r.id AS id, r.employee.name AS employeeName, r.title AS title, r.status AS status,
                   r.totalAmount AS totalAmount, r.submittedAt AS submittedAt, r.createdAt AS createdAt,
                   SIZE(r.lineItems) AS lineItemCount
            FROM ExpenseReport r
            WHERE r.employee.id = :employeeId
            ORDER BY r.createdAt DESC
            """)
    Page<ReportSummaryView> findSummariesByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    /**
     * Owner-scoped variant of {@link #findSummariesByEmployeeId} that also filters by a single
     * status, powering the "My Reports" status filter. Kept as its own query (rather than a
     * nullable-status conditional) so each path stays a simple, index-friendly statement.
     */
    @Query("""
            SELECT r.id AS id, r.employee.name AS employeeName, r.title AS title, r.status AS status,
                   r.totalAmount AS totalAmount, r.submittedAt AS submittedAt, r.createdAt AS createdAt,
                   SIZE(r.lineItems) AS lineItemCount
            FROM ExpenseReport r
            WHERE r.employee.id = :employeeId AND r.status = :status
            ORDER BY r.createdAt DESC
            """)
    Page<ReportSummaryView> findSummariesByEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                                                               @Param("status") ExpenseStatus status,
                                                               Pageable pageable);

    @Query("""
            SELECT r.id AS id, r.employee.name AS employeeName, r.title AS title, r.status AS status,
                   r.totalAmount AS totalAmount, r.submittedAt AS submittedAt, r.createdAt AS createdAt,
                   SIZE(r.lineItems) AS lineItemCount
            FROM ExpenseReport r
            WHERE r.status = :status
            ORDER BY r.submittedAt ASC
            """)
    Page<ReportSummaryView> findSummariesByStatus(@Param("status") ExpenseStatus status, Pageable pageable);

    @Query("""
            SELECT r.id AS id, r.employee.name AS employeeName, r.title AS title, r.status AS status,
                   r.totalAmount AS totalAmount, r.submittedAt AS submittedAt, r.createdAt AS createdAt,
                   SIZE(r.lineItems) AS lineItemCount
            FROM ExpenseReport r
            WHERE r.employee.manager.id = :managerId AND r.status = :status
            ORDER BY r.submittedAt ASC
            """)
    Page<ReportSummaryView> findSummariesByManagerIdAndStatus(@Param("managerId") Long managerId,
                                                               @Param("status") ExpenseStatus status,
                                                               Pageable pageable);

    /** Powers the dashboard's five "my reports by status" counts in a single round trip. */
    @Query("""
            SELECT r.status AS status, COUNT(r) AS count
            FROM ExpenseReport r
            WHERE r.employee.id = :employeeId
            GROUP BY r.status
            """)
    List<StatusCount> countGroupedByStatusForEmployee(@Param("employeeId") Long employeeId);

    long countByStatus(ExpenseStatus status);

    long countByEmployeeManagerIdAndStatus(Long managerId, ExpenseStatus status);
}
