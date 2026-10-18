package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.DashboardDTO;
import com.ghunsai.dairy.service.SalesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OWNER only. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final SalesService salesService;

    public DashboardController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/daily")
    public DashboardDTO daily() {
        return salesService.getDashboard();
    }
}
