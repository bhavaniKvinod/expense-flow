package com.expenseflow.controller;

import com.expenseflow.dto.DashboardDtos;
import com.expenseflow.security.CurrentUserProvider;
import com.expenseflow.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUserProvider currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public DashboardDtos.DashboardResponse dashboard() {
        return dashboardService.forUser(currentUser.require());
    }
}
