package com.expenseflow.service;

import com.expenseflow.dto.PolicyDtos;
import com.expenseflow.entity.PolicyConfig;
import com.expenseflow.exception.NotFoundException;
import com.expenseflow.repository.PolicyConfigRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link #get()} is called several times per request across the codebase (report submission,
 * every line item mutation, the dry-run check) for what is a single admin-editable row, so it's
 * cached. {@link #update} evicts on write and re-reads from the repository directly rather than
 * mutating the cached instance returned by {@link #get()} in place.
 */
@Service
public class PolicyConfigService {

    private static final String CACHE_NAME = "policyConfig";

    private final PolicyConfigRepository repository;

    public PolicyConfigService(PolicyConfigRepository repository) {
        this.repository = repository;
    }

    @Cacheable(CACHE_NAME)
    @Transactional(readOnly = true)
    public PolicyConfig get() {
        return fetch();
    }

    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    @Transactional
    public PolicyConfig update(PolicyDtos.UpdatePolicyRequest request) {
        PolicyConfig config = fetch();
        config.setReceiptThreshold(request.receiptThreshold());
        config.setTotalReportCap(request.totalReportCap());
        config.setMaxExpenseAgeDays(request.maxExpenseAgeDays());
        config.setUsdToInrRate(request.usdToInrRate());
        config.setUsdToEurRate(request.usdToEurRate());
        config.setUsdToAedRate(request.usdToAedRate());
        return repository.save(config);
    }

    private PolicyConfig fetch() {
        return repository.findById(1L)
                .orElseThrow(() -> new NotFoundException("Policy configuration has not been initialised"));
    }
}
