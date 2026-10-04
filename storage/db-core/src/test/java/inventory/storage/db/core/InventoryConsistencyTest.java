package inventory.storage.db.core;

import inventory.domain.product.ProductFinder;
import inventory.domain.product.ProductRegistrar;
import inventory.domain.stock.IdempotencyKeyMismatchException;
import inventory.domain.stock.InboundItem;
import inventory.domain.stock.InsufficientStockException;
import inventory.domain.stock.InventoryService;
import inventory.domain.stock.MovementType;
import inventory.domain.stock.OutboundItem;
import inventory.domain.stock.StockLockTimeoutException;
import inventory.domain.stock.StockStatus;
import inventory.storage.db.core.product.ProductRepositoryAdapter;
import inventory.storage.db.core.stock.IdempotencyRecordRepositoryAdapter;
import inventory.storage.db.core.stock.ProductStockMovementEntity;
import inventory.storage.db.core.stock.ProductStockMovementJpaRepository;
import inventory.storage.db.core.stock.ProductStockMovementRepositoryAdapter;
import inventory.storage.db.core.stock.ProductStockRepositoryAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 입고와 출고가 동시에 들어오거나 중간에 실패해도 재고 수량과 입출고 기록이 맞게 남는지 실제 PostgreSQL로 검증한다.
 * DataJpaTest 가 거는 테스트 트랜잭션을 NOT_SUPPORTED로 끈다. 서비스가 실제로 커밋해야 다른 스레드가 그 결과를 본다.
 * 롤백되지 않으니 SKU는 테스트마다 새로 만든다.
 */
@DataJpaTest
@DbContextTest
@Import({InventoryService.class, ProductFinder.class, ProductRegistrar.class,
        ProductRepositoryAdapter.class, ProductStockRepositoryAdapter.class, ProductStockMovementRepositoryAdapter.class,
        IdempotencyRecordRepositoryAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InventoryConsistencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductStockMovementJpaRepository productStockMovementJpaRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("등록되지 않은 SKU로 입고하면 상품을 등록하고 입고 수량만큼 재고를 만든다")
    void inboundNewSkuRegistersProduct() {
        String sku = newSku();

        inventoryService.inbound(new InboundItem(sku, "콜라", 10), null);

        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.product().getName()).isEqualTo("콜라");
        assertThat(status.stock().getQuantity()).isEqualTo(10);
        assertThat(movements(status.product().getId()))
                .extracting(ProductStockMovementEntity::getType, ProductStockMovementEntity::getQuantity,
                        ProductStockMovementEntity::getQuantityAfter)
                .containsExactly(tuple(MovementType.INBOUND, 10, 10));
    }

    @Test
    @DisplayName("이미 등록된 SKU로 입고하면 수량을 더하고 상품명은 바꾸지 않는다")
    void inboundRegisteredSkuAddsQuantity() {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 10), null);

        StockStatus result = inventoryService.inbound(new InboundItem(sku, "사이다", 5), null);

        assertThat(result.product().getName()).isEqualTo("콜라");
        assertThat(result.stock().getQuantity()).isEqualTo(15);
        assertThat(inventoryService.getStockBySku(sku).stock().getQuantity()).isEqualTo(15);
        assertThat(movements(result.product().getId()))
                .extracting(ProductStockMovementEntity::getType, ProductStockMovementEntity::getQuantity,
                        ProductStockMovementEntity::getQuantityAfter)
                .containsExactly(tuple(MovementType.INBOUND, 10, 10), tuple(MovementType.INBOUND, 5, 15));
    }

    @Test
    @DisplayName("등록되지 않은 SKU의 첫 입고가 동시에 들어와도 상품은 한 번만 등록되고 입고는 모두 반영된다")
    void concurrentFirstInboundRegistersProductOnce() throws Exception {
        String sku = newSku();

        runConcurrently(50, () -> inventoryService.inbound(new InboundItem(sku, "콜라", 1), null));

        // 상품이나 재고 행을 두 번 만들려 한 요청은 UNIQUE 제약에 걸려 실패한다. 50건이 모두 성공했으니 한 번씩만 만들었다
        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isEqualTo(50);
        assertThat(movements(status.product().getId()))
                .extracting(ProductStockMovementEntity::getQuantityAfter)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(1, 50));
    }

    @Test
    @DisplayName("등록된 상품에 입고가 동시에 들어와도 수량이 빠짐없이 늘어난다")
    void concurrentInboundAddsEveryQuantity() throws Exception {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 100), null);

        runConcurrently(100, () -> inventoryService.inbound(new InboundItem(sku, "콜라", 1), null));

        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isEqualTo(200);
        // 준비 입고 1건(100)과 동시 입고 100건(101~200). 값이 겹치면 두 트랜잭션이 같은 수량을 보고 계산한 것이다
        assertThat(movements(status.product().getId()))
                .extracting(ProductStockMovementEntity::getQuantityAfter)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(100, 200));
    }

    @Test
    @DisplayName("출고가 동시에 들어와도 수량이 빠짐없이 줄어든다")
    void concurrentOutboundSubtractsEveryQuantity() throws Exception {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 100), null);

        runConcurrently(100, () -> inventoryService.outbound(new OutboundItem(sku, 1), null));

        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isZero();
        // 출고 100건의 변동 뒤 수량은 99부터 0까지 한 번씩이다
        assertThat(movements(status.product().getId()))
                .filteredOn(movement -> movement.getType() == MovementType.OUTBOUND)
                .extracting(ProductStockMovementEntity::getQuantityAfter)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(0, 99));
    }

    @Test
    @DisplayName("재고보다 많은 출고가 동시에 들어오면 재고만큼만 출고하고 나머지는 거절한다")
    void concurrentOutboundBeyondStockRejectsRest() throws Exception {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 10), null);

        List<Boolean> outbounded = runConcurrently(30, () -> tryOutbound(sku));

        assertThat(outbounded).filteredOn(Boolean::booleanValue).hasSize(10);
        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isZero();
        assertThat(movements(status.product().getId()))
                .filteredOn(movement -> movement.getType() == MovementType.OUTBOUND)
                .extracting(ProductStockMovementEntity::getQuantityAfter)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(0, 9));
    }

    /**
     * 잠금을 기다리는 JDBC 호출은 인터럽트로 깨지 않는다. 별도 스레드에서 돌려야 잠금 대기 시간 제한이 걸리지 않았을 때 멈추지 않고 실패한다.
     */
    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @DisplayName("다른 트랜잭션이 재고 행을 잠그고 놓지 않으면 잠금 대기를 포기하고 아무것도 반영하지 않는다")
    void outboundGivesUpWhenStockStaysLocked() throws Exception {
        String sku = newSku();
        Long productId = inventoryService.inbound(new InboundItem(sku, "콜라", 10), null).product().getId();

        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            lockStock(lockHolder, productId);

            assertThatThrownBy(() -> inventoryService.outbound(new OutboundItem(sku, 1), null))
                    .isInstanceOf(StockLockTimeoutException.class);

            lockHolder.rollback();
        }
        assertThat(inventoryService.getStockBySku(sku).stock().getQuantity()).isEqualTo(10);
        assertThat(movements(productId)).hasSize(1);
    }

    @Test
    @DisplayName("같은 요청 키로 다시 보낸 출고는 반영하지 않고 처음 결과를 돌려준다")
    void outboundWithSameKeyIsAppliedOnce() {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 10), null);
        String key = UUID.randomUUID().toString();
        StockStatus first = inventoryService.outbound(new OutboundItem(sku, 3), key);

        StockStatus retried = inventoryService.outbound(new OutboundItem(sku, 3), key);

        assertThat(retried.stock().getQuantity()).isEqualTo(first.stock().getQuantity()).isEqualTo(7);
        assertThat(inventoryService.getStockBySku(sku).stock().getQuantity()).isEqualTo(7);
        assertThat(movements(first.product().getId()))
                .filteredOn(movement -> movement.getType() == MovementType.OUTBOUND)
                .hasSize(1);
    }

    @Test
    @DisplayName("같은 요청 키로 입고가 동시에 들어와도 한 번만 반영되고 모두 같은 결과를 받는다")
    void concurrentInboundWithSameKeyIsAppliedOnce() throws Exception {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 10), null);
        String key = UUID.randomUUID().toString();

        List<StockStatus> results = runConcurrently(10,
                () -> inventoryService.inbound(new InboundItem(sku, "콜라", 5), key));

        assertThat(results).extracting(result -> result.stock().getQuantity()).containsOnly(15);
        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isEqualTo(15);
        // 준비 입고 1건과 요청 키를 단 입고 1건만 남는다
        assertThat(movements(status.product().getId())).hasSize(2);
    }

    @Test
    @DisplayName("이미 처리한 요청 키로 수량이 다른 출고가 오면 거절하고 아무것도 반영하지 않는다")
    void outboundWithReusedKeyIsRejected() {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 10), null);
        String key = UUID.randomUUID().toString();
        inventoryService.outbound(new OutboundItem(sku, 3), key);

        assertThatThrownBy(() -> inventoryService.outbound(new OutboundItem(sku, 4), key))
                .isInstanceOf(IdempotencyKeyMismatchException.class);
        assertThat(inventoryService.getStockBySku(sku).stock().getQuantity()).isEqualTo(7);
    }

    /**
     * 스레드를 한꺼번에 출발시켜 같은 작업을 요청 수만큼 돌리고 결과를 모은다.
     * 한 건이라도 예외가 나면 Future.get()이 예외를 던져 테스트가 실패한다.
     */
    private <T> List<T> runConcurrently(int requests, Callable<T> task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }

    /**
     * 1개를 출고하고 성공 여부를 돌려준다. 재고 부족은 기대한 실패라 false로 돌려주고, 그 밖의 예외는 그대로 던진다.
     */
    private boolean tryOutbound(String sku) {
        try {
            inventoryService.outbound(new OutboundItem(sku, 1), null);
            return true;
        } catch (InsufficientStockException e) {
            return false;
        }
    }

    /**
     * 서비스와 상관없는 연결로 재고 행을 잠근다. 이 연결이 커밋이나 롤백하기 전까지 잠금이 남는다.
     */
    private static void lockStock(Connection connection, Long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM product_stock WHERE product_id = ? FOR UPDATE")) {
            statement.setLong(1, productId);
            statement.executeQuery();
        }
    }

    private List<ProductStockMovementEntity> movements(Long productId) {
        return productStockMovementJpaRepository.findAll(Sort.by("id")).stream()
                .filter(movement -> movement.getProductId().equals(productId))
                .toList();
    }

    private static String newSku() {
        return "SKU-" + UUID.randomUUID();
    }

    private static List<Integer> rangeClosed(int from, int to) {
        return IntStream.rangeClosed(from, to).boxed().toList();
    }
}
