package com.expenseflow.repository;

import com.expenseflow.entity.PolicyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyConfigRepository extends JpaRepository<PolicyConfig, Long> {
}
