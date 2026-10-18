package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.CustomerRequest;
import com.ghunsai.dairy.dto.CustomerResponse;
import com.ghunsai.dairy.entity.Customer;
import com.ghunsai.dairy.exception.ApiException;
import com.ghunsai.dairy.repository.CustomerPaymentRepository;
import com.ghunsai.dairy.repository.CustomerRepository;
import com.ghunsai.dairy.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final CustomerPaymentRepository paymentRepository;

    public CustomerService(CustomerRepository customerRepository,
                           SaleRepository saleRepository,
                           CustomerPaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<CustomerResponse> list(boolean includeInactive) {
        List<Customer> customers = includeInactive
                ? customerRepository.findAllByOrderByNameAsc()
                : customerRepository.findByActiveTrueOrderByNameAsc();
        return customers.stream().map(CustomerResponse::from).toList();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        apply(customer, request);
        customer.setActive(true);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        apply(customer, request);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse setActive(Long id, boolean active) {
        Customer customer = find(id);
        if (!active) {
            BigDecimal outstanding = saleRepository.getTotalUdhaarByCustomerId(id)
                    .subtract(paymentRepository.getTotalPaidByCustomerId(id));
            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                throw new ApiException.Conflict(
                        "Customer still owes " + outstanding + ". Collect the payment before deactivating");
            }
        }
        customer.setActive(active);
        return CustomerResponse.from(customer);
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        String phone = request.phone() == null ? null : request.phone().trim();
        customer.setPhone(phone == null || phone.isEmpty() ? null : phone);
    }

    private Customer find(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ApiException.NotFound("Customer not found"));
    }
}
