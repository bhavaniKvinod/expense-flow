package com.expenseflow.service;

import com.expenseflow.dto.PolicyDtos;
import com.expenseflow.entity.PolicyConfig;
import com.expenseflow.exception.NotFoundException;
import com.expenseflow.repository.PolicyConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyConfigService {

    private final PolicyConfigRepository repository;

    public PolicyConfigService(PolicyConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PolicyConfig get() {
        return repository.findById(1L)
                .orElseThrow(() -> new NotFoundException("Policy configuration has not been initialised"));
    }

    @Transactional
    public PolicyConfig update(PolicyDtos.UpdatePolicyRequest request) {
        PolicyConfig config = get();
        config.setReceiptThreshold(request.receiptThreshold());
        config.setTotalReportCap(request.totalReportCap());
        config.setMaxExpenseAgeDays(request.maxExpenseAgeDays());
        config.setUsdToInrRate(request.usdToInrRate());
        return repository.save(config);
    }
}
