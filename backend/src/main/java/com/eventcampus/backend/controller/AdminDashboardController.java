package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.AdminDashboardResponse;
import com.eventcampus.backend.service.AdminDashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private final AdminDashboardService dashboard;
    public AdminDashboardController(AdminDashboardService dashboard) { this.dashboard = dashboard; }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() { return dashboard.dashboard(); }
}
