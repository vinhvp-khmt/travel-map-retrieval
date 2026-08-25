package com.travelmap.api.report.controller;

import com.travelmap.api.report.dto.ReportOverview;
import com.travelmap.api.report.service.ReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports = reports; }
    @GetMapping("/api/v1/owner/reports/overview")
    ReportOverview owner(Authentication auth) { return reports.ownerOverview(auth.getName()); }
    @GetMapping("/api/v1/admin/reports/overview")
    ReportOverview admin() { return reports.adminOverview(); }
}
