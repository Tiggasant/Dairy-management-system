package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.CustomerBalanceDTO;
import com.ghunsai.dairy.dto.PaymentResponse;
import com.ghunsai.dairy.entity.Customer;
import com.ghunsai.dairy.entity.CustomerPayment;
import com.ghunsai.dairy.entity.User;
import com.ghunsai.dairy.exception.ApiException;
import com.ghunsai.dairy.repository.CustomerPaymentRepository;
import com.ghunsai.dairy.repository.CustomerRepository;
import com.ghunsai.dairy.repository.SaleRepository;
import com.ghunsai.dairy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Udhaar ledger: outstanding = total udhaar given - total payments received. */
@Service
@Transactional(readOnly = true)
public class CustomerPaymentService {

    private final CustomerPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;

    public CustomerPaymentService(CustomerPaymentRepository paymentRepository,
                                  CustomerRepository customerRepository,
                                  SaleRepository saleRepository,
                                  UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentResponse addPayment(Long customerId, BigDecimal amount, String notes, Long recordedById) {
        // Row lock so two simultaneous payments cannot both pass the "not more than owed" check.
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ApiException.NotFound("Customer not found"));
        User recordedBy = userRepository.findById(recordedById)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));

        BigDecimal outstanding = saleRepository.getTotalUdhaarByCustomerId(customerId)
                .subtract(paymentRepository.getTotalPaidByCustomerId(customerId));
        if (amount.compareTo(outstanding) > 0) {
            throw new ApiException.BadRequest(
                    "Payment exceeds the outstanding balance of " + outstanding.max(BigDecimal.ZERO));
        }

        CustomerPayment payment = new CustomerPayment();
        payment.setCustomer(customer);
        payment.setAmount(amount);
        payment.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        payment.setRecordedBy(recordedBy);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public List<PaymentResponse> getCustomerPayments(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ApiException.NotFound("Customer not found");
        }
        return paymentRepository.findByCustomerIdOrderByPaymentDateDescIdDesc(customerId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    public CustomerBalanceDTO getCustomerBalance(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ApiException.NotFound("Customer not found"));
        BigDecimal totalUdhaar = saleRepository.getTotalUdhaarByCustomerId(customerId);
        BigDecimal totalPaid = paymentRepository.getTotalPaidByCustomerId(customerId);
        return new CustomerBalanceDTO(customer.getId(), customer.getName(),
                totalUdhaar, totalPaid, totalUdhaar.subtract(totalPaid));
    }

    /** Everyone who currently owes money, biggest debt first (owner dashboard). */
    public List<CustomerBalanceDTO> getOutstandingBalances() {
        Map<Long, BigDecimal> udhaar = toMap(saleRepository.totalUdhaarGroupedByCustomer());
        Map<Long, BigDecimal> paid = toMap(paymentRepository.totalPaidGroupedByCustomer());

        return customerRepository.findAllByOrderByNameAsc().stream()
                .map(c -> {
                    BigDecimal given = udhaar.getOrDefault(c.getId(), BigDecimal.ZERO);
                    BigDecimal received = paid.getOrDefault(c.getId(), BigDecimal.ZERO);
                    return new CustomerBalanceDTO(c.getId(), c.getName(), given, received, given.subtract(received));
                })
                .filter(b -> b.outstandingBalance().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.outstandingBalance().compareTo(a.outstandingBalance()))
                .toList();
    }

    private static Map<Long, BigDecimal> toMap(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
        }
        return map;
    }
}
