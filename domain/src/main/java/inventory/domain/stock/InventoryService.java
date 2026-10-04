package inventory.domain.stock;

import inventory.domain.product.Product;
import inventory.domain.product.ProductFinder;
import inventory.domain.product.ProductRegistrar;
import inventory.domain.product.ProductRegistration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService {

    private final ProductFinder productFinder;
    private final ProductRegistrar productRegistrar;
    private final ProductStockRepository productStockRepository;
    private final ProductStockMovementRepository productStockMovementRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public InventoryService(
            ProductFinder productFinder,
            ProductRegistrar productRegistrar,
            ProductStockRepository productStockRepository,
            ProductStockMovementRepository productStockMovementRepository,
            IdempotencyRecordRepository idempotencyRecordRepository
    ) {
        this.productFinder = productFinder;
        this.productRegistrar = productRegistrar;
        this.productStockRepository = productStockRepository;
        this.productStockMovementRepository = productStockMovementRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    public StockStatus getStock(Long productId) {
        return toStockStatus(productFinder.getProduct(productId));
    }

    public StockStatus getStockBySku(String sku) {
        return toStockStatus(productFinder.getProductBySku(sku));
    }

    /**
     * 상품 등록, 재고 행 생성, 수량 변경, 기록을 한 트랜잭션에서 한다.
     * 같은 상품의 입고와 출고는 재고 행 잠금 앞에서 차례로 반영된다.
     */
    @Transactional
    public StockStatus inbound(InboundItem item, String idempotencyKey) {
        ProductRegistration registration = productRegistrar.registerIfAbsent(item.sku(), item.name());
        Product product = registration.product();
        if (registration.newlyRegistered()) {
            productStockRepository.save(ProductStock.create(product.getId()));
        }
        ProductStock locked = productStockRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> stockMissing(product));
        Optional<IdempotencyRecord> processed = findIdempotencyRecord(idempotencyKey);
        if (processed.isPresent()) {
            return replay(product, processed.get(), MovementType.INBOUND, item.quantity());
        }
        ProductStock stock = locked.inbound(item.quantity());
        productStockRepository.update(stock);
        ProductStockMovement movement = ProductStockMovement.record(
                product.getId(), MovementType.INBOUND, item.quantity(), stock.getQuantity());
        productStockMovementRepository.save(movement);
        saveIdempotencyRecord(idempotencyKey, movement);
        return new StockStatus(product, stock);
    }

    /**
     * 재고 행을 잠근 뒤에 남은 수량을 확인한다. 그래서 동시에 들어온 출고들이 같은 수량을 보고 함께 통과하지 못한다.
     * 모자라면 InsufficientStockException을 던지고, 수량도 기록도 바꾸지 않는다.
     */
    @Transactional
    public StockStatus outbound(OutboundItem item, String idempotencyKey) {
        Product product = productFinder.getProductBySku(item.sku());
        ProductStock locked = productStockRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> stockMissing(product));
        Optional<IdempotencyRecord> processed = findIdempotencyRecord(idempotencyKey);
        if (processed.isPresent()) {
            return replay(product, processed.get(), MovementType.OUTBOUND, item.quantity());
        }
        ProductStock stock = locked.outbound(item.quantity());
        productStockRepository.update(stock);
        ProductStockMovement movement = ProductStockMovement.record(
                product.getId(), MovementType.OUTBOUND, item.quantity(), stock.getQuantity());
        productStockMovementRepository.save(movement);
        saveIdempotencyRecord(idempotencyKey, movement);
        return new StockStatus(product, stock);
    }

    /**
     * 재고 행을 잠근 뒤에 불러야 한다. 같은 키로 다시 보낸 요청은 같은 재고 행을 잠그므로,
     * 동시에 와도 앞의 요청이 커밋한 기록을 보고 다시 반영하지 않는다.
     */
    private Optional<IdempotencyRecord> findIdempotencyRecord(String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
    }

    private void saveIdempotencyRecord(String idempotencyKey, ProductStockMovement movement) {
        if (idempotencyKey == null) {
            return;
        }
        idempotencyRecordRepository.save(IdempotencyRecord.of(idempotencyKey, movement.getProductId(),
                movement.getType(), movement.getQuantity(), movement.getQuantityAfter()));
    }

    /**
     * 처음 처리했을 때의 결과를 돌려준다. 수량은 지금 수량이 아니라 그때 반영한 직후의 수량이다.
     */
    private StockStatus replay(Product product, IdempotencyRecord processed, MovementType type, int quantity) {
        if (!processed.isSameRequest(product.getId(), type, quantity)) {
            throw new IdempotencyKeyMismatchException(processed.getIdempotencyKey());
        }
        return new StockStatus(product, ProductStock.of(product.getId(), processed.getQuantityAfter()));
    }

    private StockStatus toStockStatus(Product product) {
        ProductStock stock = productStockRepository.findByProductId(product.getId())
                .orElseThrow(() -> stockMissing(product));
        return new StockStatus(product, stock);
    }

    private static IllegalStateException stockMissing(Product product) {
        return new IllegalStateException("상품은 있는데 재고가 없습니다. productId=" + product.getId());
    }
}
