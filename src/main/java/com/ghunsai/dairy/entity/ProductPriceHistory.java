package com.ghunsai.dairy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_price_history")
public class ProductPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private LocalDateTime effectiveFrom;

    public ProductPriceHistory() {
    }

    public ProductPriceHistory(Product product, BigDecimal sellingPrice) {
        this.product = product;
        this.sellingPrice = sellingPrice;
    }

    @PrePersist
    void onCreate() {
        if (effectiveFrom == null) {
            effectiveFrom = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
}
