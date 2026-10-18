package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.SaleDetailResponse;
import com.ghunsai.dairy.dto.SaleItemRequest;
import com.ghunsai.dairy.dto.SaleRequest;
import com.ghunsai.dairy.entity.Customer;
import com.ghunsai.dairy.entity.Product;
import com.ghunsai.dairy.entity.Role;
import com.ghunsai.dairy.entity.Sale;
import com.ghunsai.dairy.entity.SaleItem;
import com.ghunsai.dairy.entity.StockMovement;
import com.ghunsai.dairy.entity.User;
import com.ghunsai.dairy.exception.ApiException;
import com.ghunsai.dairy.repository.CustomerRepository;
import com.ghunsai.dairy.repository.ProductRepository;
import com.ghunsai.dairy.repository.SaleItemRepository;
import com.ghunsai.dairy.repository.SaleRepository;
import com.ghunsai.dairy.repository.StockMovementRepository;
import com.ghunsai.dairy.repository.UserRepository;
import com.ghunsai.dairy.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesServiceTest {

    @Mock ProductRepository productRepository;
    @Mock SaleRepository saleRepository;
    @Mock SaleItemRepository saleItemRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock CustomerRepository customerRepository;
    @Mock UserRepository userRepository;
    @Mock StockService stockService;

    @Captor ArgumentCaptor<List<SaleItem>> itemsCaptor;
    @Captor ArgumentCaptor<List<StockMovement>> movementsCaptor;

    SalesService service;
    User worker;
    AppUserPrincipal principal;
    Product milk;

    @BeforeEach
    void setUp() {
        service = new SalesService(productRepository, saleRepository, saleItemRepository,
                stockMovementRepository, customerRepository, userRepository, stockService);

        worker = new User("worker1", "Worker One", "hash", Role.WORKER);
        ReflectionTestUtils.setField(worker, "id", 7L);
        principal = new AppUserPrincipal(7L, "worker1", "Worker One", Role.WORKER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(worker));

        milk = new Product();
        ReflectionTestUtils.setField(milk, "id", 1L);
        milk.setName("Milk");
        milk.setUnit("litre");
        milk.setSellingPrice(new BigDecimal("50.00"));
        when(productRepository.findActiveProductForUpdate(1L)).thenReturn(Optional.of(milk));
        when(stockService.availableStock(1L)).thenReturn(new BigDecimal("10.000"));

        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> {
            Sale sale = inv.getArgument(0);
            ReflectionTestUtils.setField(sale, "id", 100L);
            return sale;
        });
    }

    private static SaleRequest request(List<SaleItemRequest> items, String cash, String upi, String udhaar,
                                       Long customerId, String clientRequestId) {
        return new SaleRequest(items, new BigDecimal(cash), new BigDecimal(upi), new BigDecimal(udhaar),
                customerId, clientRequestId);
    }

    private static SaleItemRequest line(long productId, String qty) {
        return new SaleItemRequest(productId, new BigDecimal(qty));
    }

    @Test
    void createsBillUsingServerSidePriceAndDeductsStock() {
        SaleDetailResponse result = service.createSale(
                request(List.of(line(1, "2")), "60.00", "40.00", "0", null, null), principal);

        assertEquals(new BigDecimal("100.00"), result.sale().totalAmount());
        assertEquals(100L, result.sale().id());
        assertEquals("worker1", result.sale().createdBy());
        assertEquals(1, result.items().size());

        verify(stockMovementRepository).saveAll(movementsCaptor.capture());
        StockMovement movement = movementsCaptor.getValue().get(0);
        assertEquals(new BigDecimal("2"), movement.getQuantity());
        assertEquals("Sale #100", movement.getNotes());
    }

    @Test
    void sameProductOnTwoLinesIsMergedIntoOne() {
        service.createSale(
                request(List.of(line(1, "1"), line(1, "2.5")), "175.00", "0", "0", null, null), principal);

        verify(saleItemRepository).saveAll(itemsCaptor.capture());
        assertEquals(1, itemsCaptor.getValue().size());
        assertEquals(0, new BigDecimal("3.5").compareTo(itemsCaptor.getValue().get(0).getQuantity()));
    }

    @Test
    void rejectsWhenPaymentSplitDoesNotMatchTotal() {
        ApiException ex = assertThrows(ApiException.class, () -> service.createSale(
                request(List.of(line(1, "2")), "50.00", "0", "0", null, null), principal));
        assertTrue(ex.getMessage().contains("must equal"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rejectsWhenStockIsInsufficient() {
        ApiException ex = assertThrows(ApiException.class, () -> service.createSale(
                request(List.of(line(1, "11")), "550.00", "0", "0", null, null), principal));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void rejectsUdhaarWithoutCustomer() {
        ApiException ex = assertThrows(ApiException.class, () -> service.createSale(
                request(List.of(line(1, "2")), "0", "0", "100.00", null, null), principal));
        assertTrue(ex.getMessage().contains("Customer is required"));
    }

    @Test
    void rejectsInactiveCustomer() {
        Customer inactive = new Customer();
        inactive.setActive(false);
        when(customerRepository.findById(5L)).thenReturn(Optional.of(inactive));

        assertThrows(ApiException.class, () -> service.createSale(
                request(List.of(line(1, "2")), "0", "0", "100.00", 5L, null), principal));
    }

    @Test
    void rejectsUnknownOrInactiveProduct() {
        when(productRepository.findActiveProductForUpdate(99L)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.createSale(
                request(List.of(line(99, "1")), "50.00", "0", "0", null, null), principal));
    }

    @Test
    void retriedRequestReturnsExistingBillInsteadOfCreatingDuplicate() {
        Sale existing = new Sale();
        ReflectionTestUtils.setField(existing, "id", 55L);
        existing.setTotalAmount(new BigDecimal("100.00"));
        existing.setCashAmount(new BigDecimal("100.00"));
        existing.setUpiAmount(BigDecimal.ZERO);
        existing.setUdhaarAmount(BigDecimal.ZERO);
        existing.setCreatedBy(worker);
        when(saleRepository.findByCreatedByIdAndClientRequestId(7L, "req-1")).thenReturn(Optional.of(existing));
        when(saleItemRepository.findBySaleIdOrderByIdAsc(anyLong())).thenReturn(List.of());

        SaleDetailResponse result = service.createSale(
                request(List.of(line(1, "2")), "100.00", "0", "0", null, "req-1"), principal);

        assertEquals(55L, result.sale().id());
        verify(saleRepository, never()).save(any(Sale.class));
        verify(stockMovementRepository, never()).saveAll(any());
    }
}
