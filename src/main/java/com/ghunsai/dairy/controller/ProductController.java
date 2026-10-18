package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.ActiveRequest;
import com.ghunsai.dairy.dto.ProductPriceHistoryDTO;
import com.ghunsai.dairy.dto.ProductRequest;
import com.ghunsai.dairy.dto.ProductResponse;
import com.ghunsai.dairy.dto.UpdatePriceRequest;
import com.ghunsai.dairy.security.AppUserPrincipal;
import com.ghunsai.dairy.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** OWNER + WORKER (worker sees active products only). */
    @GetMapping
    public List<ProductResponse> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return productService.list(user.isOwner());
    }

    /** OWNER + WORKER. */
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return productService.get(id, user.isOwner());
    }

    /** OWNER only. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    /** OWNER only. */
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    /** OWNER only. */
    @PutMapping("/{id}/price")
    public ProductResponse updatePrice(@PathVariable Long id, @Valid @RequestBody UpdatePriceRequest request) {
        return productService.updatePrice(id, request.sellingPrice());
    }

    /** OWNER only: hide a product from the billing counter without deleting its history. */
    @PutMapping("/{id}/active")
    public ProductResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return productService.setActive(id, request.active());
    }

    /** OWNER only. */
    @GetMapping("/{id}/price-history")
    public List<ProductPriceHistoryDTO> priceHistory(@PathVariable Long id) {
        return productService.priceHistory(id);
    }
}
