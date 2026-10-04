package inventory.domain.stock;

import inventory.domain.product.Product;
import inventory.domain.product.ProductFinder;
import inventory.domain.product.ProductRegistrar;
import inventory.domain.product.ProductRegistration;
import inventory.storage.db.core.stock.ProductStockEntity;
import inventory.storage.db.core.stock.ProductStockJpaRepository;
import inventory.storage.db.core.stock.ProductStockMovementEntity;
import inventory.storage.db.core.stock.ProductStockMovementJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductFinder productFinder;
    private final ProductRegistrar productRegistrar;
    private final ProductStockJpaRepository productStockJpaRepository;
    private final ProductStockMovementJpaRepository productStockMovementJpaRepository;

    public InventoryService(
            ProductFinder productFinder,
            ProductRegistrar productRegistrar,
            ProductStockJpaRepository productStockJpaRepository,
            ProductStockMovementJpaRepository productStockMovementJpaRepository
    ) {
        this.productFinder = productFinder;
        this.productRegistrar = productRegistrar;
        this.productStockJpaRepository = productStockJpaRepository;
        this.productStockMovementJpaRepository = productStockMovementJpaRepository;
    }

    @Transactional(readOnly = true)
    public StockStatus getStock(Long productId) {
        return toStockStatus(productFinder.getProduct(productId));
    }

    @Transactional(readOnly = true)
    public StockStatus getStockBySku(String sku) {
        return toStockStatus(productFinder.getProductBySku(sku));
    }

    /**
     * 상품 등록, 재고 행 생성, 수량 변경, 기록을 한 트랜잭션에서 한다.
     * 같은 상품의 입고와 출고는 재고 행 잠금 앞에서 차례로 반영된다.
     */
    @Transactional
    public StockStatus inbound(InboundItem item) {
        ProductRegistration registration = productRegistrar.registerIfAbsent(item.sku(), item.name());
        Product product = registration.product();
        if (registration.newlyRegistered()) {
            productStockJpaRepository.save(toEntity(ProductStock.create(product.getId())));
        }
        ProductStockEntity stockEntity = productStockJpaRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> stockMissing(product));
        ProductStock stock = toProductStock(stockEntity).inbound(item.quantity());
        stockEntity.updateQuantity(stock.getQuantity());
        ProductStockMovement movement = ProductStockMovement.record(
                product.getId(), MovementType.INBOUND, item.quantity(), stock.getQuantity());
        productStockMovementJpaRepository.save(toEntity(movement));
        return new StockStatus(product, stock);
    }

    /**
     * 재고 행을 잠근 뒤에 남은 수량을 확인한다. 그래서 동시에 들어온 출고들이 같은 수량을 보고 함께 통과하지 못한다.
     * 모자라면 InsufficientStockException을 던지고, 수량도 기록도 바꾸지 않는다.
     */
    @Transactional
    public StockStatus outbound(OutboundItem item) {
        Product product = productFinder.getProductBySku(item.sku());
        ProductStockEntity stockEntity = productStockJpaRepository.findByProductIdForUpdate(product.getId())
                .orElseThrow(() -> stockMissing(product));
        ProductStock stock = toProductStock(stockEntity).outbound(item.quantity());
        stockEntity.updateQuantity(stock.getQuantity());
        ProductStockMovement movement = ProductStockMovement.record(
                product.getId(), MovementType.OUTBOUND, item.quantity(), stock.getQuantity());
        productStockMovementJpaRepository.save(toEntity(movement));
        return new StockStatus(product, stock);
    }

    private StockStatus toStockStatus(Product product) {
        ProductStock stock = productStockJpaRepository.findByProductId(product.getId())
                .map(InventoryService::toProductStock)
                .orElseThrow(() -> stockMissing(product));
        return new StockStatus(product, stock);
    }

    private static IllegalStateException stockMissing(Product product) {
        return new IllegalStateException("상품은 있는데 재고가 없습니다. productId=" + product.getId());
    }

    private static ProductStock toProductStock(ProductStockEntity entity) {
        return ProductStock.of(entity.getProductId(), entity.getQuantity());
    }

    private static ProductStockEntity toEntity(ProductStock stock) {
        return new ProductStockEntity(stock.getProductId(), stock.getQuantity());
    }

    private static ProductStockMovementEntity toEntity(ProductStockMovement movement) {
        return new ProductStockMovementEntity(
                movement.getProductId(), movement.getType().name(), movement.getQuantity(), movement.getQuantityAfter());
    }
}
