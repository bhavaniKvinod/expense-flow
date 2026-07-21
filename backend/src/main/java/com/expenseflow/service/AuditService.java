package com.expenseflow.service;

import com.expenseflow.domain.AuditAction;
import com.expenseflow.entity.AuditEvent;
import com.expenseflow.entity.ExpenseReport;
import com.expenseflow.entity.User;
import com.expenseflow.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(ExpenseReport report, User actor, AuditAction action, String note) {
        auditEventRepository.save(new AuditEvent(report, actor, action, note));
    }

    public List<AuditEvent> forReport(Long reportId) {
        return auditEventRepository.findByReportIdOrderByCreatedAtAsc(reportId);
    }
}
