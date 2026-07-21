package com.expenseflow.config;

import com.expenseflow.domain.Role;
import com.expenseflow.entity.PolicyConfig;
import com.expenseflow.entity.User;
import com.expenseflow.repository.PolicyConfigRepository;
import com.expenseflow.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds a default policy config and one demo user per role on first startup.
 * Idempotent: only inserts when the relevant table is empty.
 *
 * Demo credentials (all share the same password): password123
 *   admin@expenseflow.test    (ADMIN / Finance)
 *   manager@expenseflow.test  (MANAGER)
 *   employee@expenseflow.test (EMPLOYEE, reports to the manager above)
 *   dev@expenseflow.test      (EMPLOYEE, reports to the manager above)
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PolicyConfigRepository policyConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      PolicyConfigRepository policyConfigRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.policyConfigRepository = policyConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedPolicyConfig();
        seedUsers();
    }

    private void seedPolicyConfig() {
        if (policyConfigRepository.existsById(1L)) {
            return;
        }
        PolicyConfig config = new PolicyConfig();
        config.setId(1L);
        config.setReceiptThreshold(new BigDecimal("25.00"));
        config.setTotalReportCap(new BigDecimal("5000.00"));
        config.setMaxExpenseAgeDays(90);
        config.setUsdToInrRate(new BigDecimal("83.0000"));
        policyConfigRepository.save(config);
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = newUser("Avery Admin", "admin@expenseflow.test", Role.ADMIN, null);
        User manager = newUser("Morgan Manager", "manager@expenseflow.test", Role.MANAGER, null);
        userRepository.save(admin);
        userRepository.save(manager);

        userRepository.save(newUser("Emma Employee", "employee@expenseflow.test", Role.EMPLOYEE, manager));
        userRepository.save(newUser("Devon Dev", "dev@expenseflow.test", Role.EMPLOYEE, manager));
    }

    private User newUser(String name, String email, Role role, User manager) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        u.setRole(role);
        u.setManager(manager);
        u.setActive(true);
        return u;
    }
}
