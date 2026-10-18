package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.PaymentRequest;
import com.ghunsai.dairy.dto.PaymentResponse;
import com.ghunsai.dairy.security.AppUserPrincipal;
import com.ghunsai.dairy.service.CustomerPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Udhaar repayments. OWNER + WORKER. */
@RestController
@RequestMapping("/api/customer-payments")
public class CustomerPaymentController {

    private final CustomerPaymentService paymentService;

    public CustomerPaymentController(CustomerPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{customerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse addPayment(@PathVariable Long customerId,
                                      @Valid @RequestBody PaymentRequest request,
                                      @AuthenticationPrincipal AppUserPrincipal user) {
        return paymentService.addPayment(customerId, request.amount(), request.notes(), user.id());
    }

    @GetMapping("/{customerId}")
    public List<PaymentResponse> list(@PathVariable Long customerId) {
        return paymentService.getCustomerPayments(customerId);
    }
}
