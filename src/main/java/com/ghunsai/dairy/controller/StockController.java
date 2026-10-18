package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.AdjustmentRequest;
import com.ghunsai.dairy.dto.PageResponse;
import com.ghunsai.dairy.dto.PurchaseRequest;
import com.ghunsai.dairy.dto.StockLevelResponse;
import com.ghunsai.dairy.dto.StockMovementResponse;
import com.ghunsai.dairy.dto.StockReportDTO;
import com.ghunsai.dairy.security.AppUserPrincipal;
import com.ghunsai.dairy.service.StockService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /** OWNER + WORKER. */
    @GetMapping("/{productId}")
    public StockLevelResponse level(@PathVariable Long productId) {
        return stockService.getStockLevel(productId);
    }

    /** OWNER + WORKER. */
    @GetMapping("/report")
    public List<StockReportDTO> report() {
        return stockService.getStockReport();
    }

    /** OWNER only: goods received. */
    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse purchase(@Valid @RequestBody PurchaseRequest request,
                                          @AuthenticationPrincipal AppUserPrincipal user) {
        return stockService.addPurchase(request, user.id());
    }

    /** OWNER only: wastage / manual correction. */
    @PostMapping("/adjust")
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse adjust(@Valid @RequestBody AdjustmentRequest request,
                                        @AuthenticationPrincipal AppUserPrincipal user) {
        return stockService.adjust(request, user.id());
    }

    /** OWNER only: stock ledger, newest first. */
    @GetMapping("/movements")
    public PageResponse<StockMovementResponse> movements(
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return stockService.movements(productId, page, size);
    }
}
