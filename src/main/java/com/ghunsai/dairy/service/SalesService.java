package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.DashboardDTO;
import com.ghunsai.dairy.dto.PageResponse;
import com.ghunsai.dairy.dto.SaleDetailResponse;
import com.ghunsai.dairy.dto.SaleItemRequest;
import com.ghunsai.dairy.dto.SaleItemResponse;
import com.ghunsai.dairy.dto.SaleRequest;
import com.ghunsai.dairy.dto.SaleResponse;
import com.ghunsai.dairy.dto.SalesReportDTO;
import com.ghunsai.dairy.entity.Customer;
import com.ghunsai.dairy.entity.MovementType;
import com.ghunsai.dairy.entity.Product;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class SalesService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final StockService stockService;

    public SalesService(ProductRepository productRepository,
                        SaleRepository saleRepository,
                        SaleItemRepository saleItemRepository,
                        StockMovementRepository stockMovementRepository,
                        CustomerRepository customerRepository,
                        UserRepository userRepository,
                        StockService stockService) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.stockService = stockService;
    }

    /**
     * Creates a bill atomically: validates stock, prices the lines with the CURRENT server-side
     * price, checks cash + UPI + udhaar == total, and writes the stock deductions.
     */
    @Transactional
    public SaleDetailResponse createSale(SaleRequest request, AppUserPrincipal principal) {
        // Idempotency: the app retried a request that already succeeded -> return the same bill.
        if (request.clientRequestId() != null) {
            Sale existing = saleRepository
                    .findByCreatedByIdAndClientRequestId(principal.id(), request.clientRequestId())
                    .orElse(null);
            if (existing != null) {
                return detailOf(existing);
            }
        }

        User creator = userRepository.findById(principal.id())
                .orElseThrow(() -> new ApiException.NotFound("User not found"));

        // Same product on several lines -> merge. Sorted by id -> every transaction locks products
        // in the same order, so two simultaneous bills can never deadlock each other.
        Map<Long, BigDecimal> quantities = new TreeMap<>();
        for (SaleItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), BigDecimal::add);
        }

        List<SaleItem> items = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, BigDecimal> line : quantities.entrySet()) {
            Product product = productRepository.findActiveProductForUpdate(line.getKey())
                    .orElseThrow(() -> new ApiException.BadRequest("Active product not found: " + line.getKey()));
            BigDecimal quantity = line.getValue();

            BigDecimal available = stockService.availableStock(product.getId());
            if (available.compareTo(quantity) < 0) {
                throw new ApiException.BadRequest("Insufficient stock for " + product.getName()
                        + ". Available: " + available + " " + product.getUnit());
            }

            BigDecimal price = product.getSellingPrice();
            BigDecimal subtotal = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setSellingPrice(price);
            item.setSubtotal(subtotal);
            items.add(item);

            StockMovement movement = new StockMovement();
            movement.setProduct(product);
            movement.setMovementType(MovementType.SALE);
            movement.setQuantity(quantity);
            movement.setCreatedBy(creator);
            movements.add(movement);
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException.BadRequest("Bill total must be greater than zero");
        }

        BigDecimal cash = request.cashAmount();
        BigDecimal upi = request.upiAmount();
        BigDecimal udhaar = request.udhaarAmount();
        if (cash.add(upi).add(udhaar).compareTo(total) != 0) {
            throw new ApiException.BadRequest(
                    "Cash + UPI + Udhaar must equal the bill total of " + total);
        }

        Customer customer = null;
        if (request.customerId() != null) {
            customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new ApiException.BadRequest("Customer not found"));
            if (!customer.isActive()) {
                throw new ApiException.BadRequest("Customer is inactive");
            }
        }
        if (udhaar.compareTo(BigDecimal.ZERO) > 0 && customer == null) {
            throw new ApiException.BadRequest("Customer is required for an udhaar sale");
        }

        Sale sale = new Sale();
        sale.setTotalAmount(total);
        sale.setCashAmount(cash);
        sale.setUpiAmount(upi);
        sale.setUdhaarAmount(udhaar);
        sale.setCustomer(customer);
        sale.setCreatedBy(creator);
        sale.setClientRequestId(request.clientRequestId());
        Sale saved = saleRepository.save(sale);

        for (SaleItem item : items) {
            item.setSale(saved);
        }
        for (StockMovement movement : movements) {
            movement.setNotes("Sale #" + saved.getId());
        }
        saleItemRepository.saveAll(items);
        stockMovementRepository.saveAll(movements);

        return new SaleDetailResponse(SaleResponse.from(saved),
                items.stream().map(SaleItemResponse::from).toList());
    }

    /** Owner: paged bill history. Defaults to the last 30 days. */
    public PageResponse<SaleResponse> listSales(LocalDate from, LocalDate to, int page, int size) {
        LocalDate toDate = to != null ? to : LocalDate.now();
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);
        if (fromDate.isAfter(toDate)) {
            throw new ApiException.BadRequest("'from' must not be after 'to'");
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), StockService.clampSize(size),
                Sort.by(Sort.Direction.DESC, "saleDate").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Sale> result = saleRepository.findBySaleDateGreaterThanEqualAndSaleDateLessThan(
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay(), pageable);
        return PageResponse.of(result, SaleResponse::from);
    }

    /** A worker may only open bills he created himself; the owner can open any bill. */
    public SaleDetailResponse getSale(Long id, AppUserPrincipal principal) {
        Sale sale = saleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ApiException.NotFound("Sale not found"));
        if (!principal.isOwner() && !sale.getCreatedBy().getId().equals(principal.id())) {
            throw new AccessDeniedException("Not your bill");
        }
        return detailOf(sale);
    }

    /** All bills of a day (owner). */
    public SalesReportDTO getDailyReport(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now();
        return toReport(day, saleRepository.summarize(day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
    }

    /** The logged-in user's own bills of a day: what a worker has to hand over at closing time. */
    public SalesReportDTO getMyDailyReport(LocalDate date, Long userId) {
        LocalDate day = date != null ? date : LocalDate.now();
        return toReport(day, saleRepository.summarizeByUser(
                day.atStartOfDay(), day.plusDays(1).atStartOfDay(), userId));
    }

    public DashboardDTO getDashboard() {
        SalesReportDTO r = getDailyReport(null);
        return new DashboardDTO(r.totalSales(), r.cashCollection(), r.upiCollection(), r.udhaarAmount(), r.totalBills());
    }

    private SaleDetailResponse detailOf(Sale sale) {
        List<SaleItemResponse> items = saleItemRepository.findBySaleIdOrderByIdAsc(sale.getId()).stream()
                .map(SaleItemResponse::from)
                .toList();
        return new SaleDetailResponse(SaleResponse.from(sale), items);
    }

    private static SalesReportDTO toReport(LocalDate day, List<Object[]> rows) {
        Object[] r = rows.isEmpty() ? new Object[]{0L, null, null, null, null} : rows.get(0);
        return new SalesReportDTO(day,
                ((Number) r[0]).longValue(), money(r[1]), money(r[2]), money(r[3]), money(r[4]));
    }

    private static BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : (BigDecimal) value;
    }
}
