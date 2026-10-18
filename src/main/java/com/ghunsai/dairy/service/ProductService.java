package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.ProductPriceHistoryDTO;
import com.ghunsai.dairy.dto.ProductRequest;
import com.ghunsai.dairy.dto.ProductResponse;
import com.ghunsai.dairy.entity.Product;
import com.ghunsai.dairy.entity.ProductPriceHistory;
import com.ghunsai.dairy.exception.ApiException;
import com.ghunsai.dairy.repository.ProductPriceHistoryRepository;
import com.ghunsai.dairy.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPriceHistoryRepository historyRepository;

    public ProductService(ProductRepository productRepository, ProductPriceHistoryRepository historyRepository) {
        this.productRepository = productRepository;
        this.historyRepository = historyRepository;
    }

    /** Workers only see active products; the owner sees everything. */
    public List<ProductResponse> list(boolean includeInactive) {
        List<Product> products = includeInactive
                ? productRepository.findAllByOrderByNameAsc()
                : productRepository.findByActiveTrueOrderByNameAsc();
        return products.stream().map(ProductResponse::from).toList();
    }

    public ProductResponse get(Long id, boolean includeInactive) {
        Product product = find(id);
        if (!product.isActive() && !includeInactive) {
            throw new ApiException.NotFound("Product not found");
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        product.setSellingPrice(request.sellingPrice());
        product.setActive(true);
        Product saved = productRepository.save(product);
        historyRepository.save(new ProductPriceHistory(saved, saved.getSellingPrice()));
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        changePriceIfNeeded(product, request.sellingPrice());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updatePrice(Long id, BigDecimal newPrice) {
        Product product = find(id);
        changePriceIfNeeded(product, newPrice);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse setActive(Long id, boolean active) {
        Product product = find(id);
        product.setActive(active);
        return ProductResponse.from(product);
    }

    public List<ProductPriceHistoryDTO> priceHistory(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ApiException.NotFound("Product not found");
        }
        return historyRepository.findByProductIdOrderByEffectiveFromDescIdDesc(productId).stream()
                .map(h -> new ProductPriceHistoryDTO(h.getId(), productId, h.getSellingPrice(), h.getEffectiveFrom()))
                .toList();
    }

    private void changePriceIfNeeded(Product product, BigDecimal newPrice) {
        if (product.getSellingPrice().compareTo(newPrice) != 0) {
            product.setSellingPrice(newPrice);
            historyRepository.save(new ProductPriceHistory(product, newPrice));
        }
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setCategory(request.category().trim());
        product.setUnit(request.unit().trim());
        product.setLowStockThreshold(request.lowStockThreshold());
    }

    private Product find(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ApiException.NotFound("Product not found"));
    }
}
