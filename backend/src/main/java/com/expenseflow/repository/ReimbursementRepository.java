package com.expenseflow.repository;

import com.expenseflow.entity.Reimbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long> {
    Optional<Reimbursement> findByReportId(Long reportId);
}
