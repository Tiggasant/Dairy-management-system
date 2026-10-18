package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.AdjustmentRequest;
import com.ghunsai.dairy.dto.PageResponse;
import com.ghunsai.dairy.dto.PurchaseRequest;
import com.ghunsai.dairy.dto.StockLevelResponse;
import com.ghunsai.dairy.dto.StockMovementResponse;
import com.ghunsai.dairy.dto.StockReportDTO;
import com.ghunsai.dairy.entity.MovementType;
import com.ghunsai.dairy.entity.Product;
import com.ghunsai.dairy.entity.StockMovement;
import com.ghunsai.dairy.entity.User;
import com.ghunsai.dairy.exception.ApiException;
import com.ghunsai.dairy.repository.ProductRepository;
import com.ghunsai.dairy.repository.StockMovementRepository;
import com.ghunsai.dairy.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stock is never stored as a number: it is always derived from the movement ledger
 * (purchase / adjustment-in add, sale / wastage / adjustment-out subtract).
 */
@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public StockService(StockMovementRepository movementRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public BigDecimal availableStock(Long productId) {
        BigDecimal stock = BigDecimal.ZERO;
        for (Object[] row : movementRepository.sumByType(productId)) {
            stock = apply(stock, (MovementType) row[0], (BigDecimal) row[1]);
        }
        return stock;
    }

    public StockLevelResponse getStockLevel(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ApiException.NotFound("Product not found");
        }
        return new StockLevelResponse(productId, availableStock(productId));
    }

    public List<StockReportDTO> getStockReport() {
        Map<Long, BigDecimal> stockByProduct = new HashMap<>();
        for (Object[] row : movementRepository.sumByProductAndType()) {
            Long productId = ((Number) row[0]).longValue();
            BigDecimal current = stockByProduct.getOrDefault(productId, BigDecimal.ZERO);
            stockByProduct.put(productId, apply(current, (MovementType) row[1], (BigDecimal) row[2]));
        }
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(p -> {
                    BigDecimal available = stockByProduct.getOrDefault(p.getId(), BigDecimal.ZERO);
                    return new StockReportDTO(p.getId(), p.getName(), p.getUnit(), available,
                            p.getLowStockThreshold(), available.compareTo(p.getLowStockThreshold()) <= 0);
                })
                .toList();
    }

    @Transactional
    public StockMovementResponse addPurchase(PurchaseRequest request, Long userId) {
        Product product = lockActiveProduct(request.productId());
        return StockMovementResponse.from(
                record(product, MovementType.PURCHASE, request.quantity(), request.notes(), userId));
    }

    @Transactional
    public StockMovementResponse adjust(AdjustmentRequest request, Long userId) {
        if (request.type() == MovementType.PURCHASE || request.type() == MovementType.SALE) {
            throw new ApiException.BadRequest(
                    "Type must be WASTAGE, ADJUSTMENT_IN or ADJUSTMENT_OUT");
        }
        Product product = lockActiveProduct(request.productId());
        if (!request.type().isInbound()) {
            BigDecimal available = availableStock(product.getId());
            if (available.compareTo(request.quantity()) < 0) {
                throw new ApiException.BadRequest(
                        "Cannot remove more than the available stock (" + available + ")");
            }
        }
        return StockMovementResponse.from(
                record(product, request.type(), request.quantity(), request.notes(), userId));
    }

    public PageResponse<StockMovementResponse> movements(Long productId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<StockMovement> result = productId == null
                ? movementRepository.findAll(pageable)
                : movementRepository.findByProductId(productId, pageable);
        return PageResponse.of(result, StockMovementResponse::from);
    }

    private StockMovement record(Product product, MovementType type, BigDecimal quantity, String notes, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        movement.setCreatedBy(user);
        return movementRepository.save(movement);
    }

    private Product lockActiveProduct(Long productId) {
        return productRepository.findActiveProductForUpdate(productId)
                .orElseThrow(() -> new ApiException.NotFound("Active product not found: " + productId));
    }

    private static BigDecimal apply(BigDecimal stock, MovementType type, BigDecimal quantity) {
        return type.isInbound() ? stock.add(quantity) : stock.subtract(quantity);
    }

    static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
