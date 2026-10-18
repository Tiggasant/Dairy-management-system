package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.Customer;

public record CustomerResponse(Long id, String name, String phone, boolean active) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getPhone(), c.isActive());
    }
}
