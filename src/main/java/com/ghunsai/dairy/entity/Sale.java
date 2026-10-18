package com.ghunsai.dairy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime saleDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cashAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal upiAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal udhaarAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** The worker / owner who created this bill. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(length = 64)
    private String clientRequestId;

    public Sale() {
    }

    @PrePersist
    void onCreate() {
        if (saleDate == null) {
            saleDate = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public LocalDateTime getSaleDate() { return saleDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getCashAmount() { return cashAmount; }
    public void setCashAmount(BigDecimal cashAmount) { this.cashAmount = cashAmount; }
    public BigDecimal getUpiAmount() { return upiAmount; }
    public void setUpiAmount(BigDecimal upiAmount) { this.upiAmount = upiAmount; }
    public BigDecimal getUdhaarAmount() { return udhaarAmount; }
    public void setUdhaarAmount(BigDecimal udhaarAmount) { this.udhaarAmount = udhaarAmount; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
}
