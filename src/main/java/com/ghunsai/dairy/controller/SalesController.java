package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.PageResponse;
import com.ghunsai.dairy.dto.SaleDetailResponse;
import com.ghunsai.dairy.dto.SaleRequest;
import com.ghunsai.dairy.dto.SaleResponse;
import com.ghunsai.dairy.dto.SalesReportDTO;
import com.ghunsai.dairy.security.AppUserPrincipal;
import com.ghunsai.dairy.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    /** OWNER + WORKER: create a bill. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleDetailResponse create(@Valid @RequestBody SaleRequest request,
                                     @AuthenticationPrincipal AppUserPrincipal user) {
        return salesService.createSale(request, user);
    }

    /** OWNER only: paged bill history (default: last 30 days). */
    @GetMapping
    public PageResponse<SaleResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return salesService.listSales(from, to, page, size);
    }

    /** OWNER (any bill) or WORKER (only his own bills): receipt with line items. */
    @GetMapping("/{id}")
    public SaleDetailResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return salesService.getSale(id, user);
    }

    /** OWNER only: totals of the whole shop for a day (default today). */
    @GetMapping("/reports/daily")
    public SalesReportDTO dailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return salesService.getDailyReport(date);
    }

    /** OWNER + WORKER: totals of the caller's own bills for a day (cash to hand over). */
    @GetMapping("/reports/my-daily")
    public SalesReportDTO myDailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AppUserPrincipal user) {
        return salesService.getMyDailyReport(date, user.id());
    }
}
