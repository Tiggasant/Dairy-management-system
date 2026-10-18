package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.ActiveRequest;
import com.ghunsai.dairy.dto.CustomerBalanceDTO;
import com.ghunsai.dairy.dto.CustomerRequest;
import com.ghunsai.dairy.dto.CustomerResponse;
import com.ghunsai.dairy.security.AppUserPrincipal;
import com.ghunsai.dairy.service.CustomerPaymentService;
import com.ghunsai.dairy.service.CustomerService;
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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerPaymentService paymentService;

    public CustomerController(CustomerService customerService, CustomerPaymentService paymentService) {
        this.customerService = customerService;
        this.paymentService = paymentService;
    }

    /** OWNER + WORKER (worker sees active customers only). */
    @GetMapping
    public List<CustomerResponse> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return customerService.list(user.isOwner());
    }

    /** OWNER + WORKER. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    /** OWNER only. */
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    /** OWNER only. */
    @PutMapping("/{id}/active")
    public CustomerResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return customerService.setActive(id, request.active());
    }

    /** OWNER + WORKER. */
    @GetMapping("/{id}/balance")
    public CustomerBalanceDTO balance(@PathVariable Long id) {
        return paymentService.getCustomerBalance(id);
    }

    /** OWNER only: everybody who owes money, biggest debt first. */
    @GetMapping("/balances")
    public List<CustomerBalanceDTO> outstandingBalances() {
        return paymentService.getOutstandingBalances();
    }
}
