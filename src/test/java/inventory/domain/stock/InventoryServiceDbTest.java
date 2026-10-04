package inventory.domain.stock;

import inventory.DbContextTest;
import inventory.domain.product.ProductFinder;
import inventory.domain.product.ProductRegistrar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataJpaTest 가 거는 테스트 트랜잭션을 NOT_SUPPORTED로 끈다. 서비스가 실제로 커밋해야 다른 스레드가 그 결과를 본다.
 * 롤백되지 않으니 SKU는 테스트마다 새로 만든다.
 */
@DataJpaTest
@DbContextTest
@Import({InventoryService.class, ProductFinder.class, ProductRegistrar.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InventoryServiceDbTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("등록되지 않은 SKU로 입고하면 상품을 등록하고 입고 수량만큼 재고를 만든다")
    void inboundNewSkuRegistersProduct() {
        String sku = newSku();

        inventoryService.inbound(new InboundItem(sku, "콜라", 10));

        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.product().getName()).isEqualTo("콜라");
        assertThat(status.stock().getQuantity()).isEqualTo(10);
        assertThat(movements(status.product().getId()))
                .containsExactly(new MovementRow("INBOUND", 10, 10));
    }

    @Test
    @DisplayName("이미 등록된 SKU로 입고하면 수량을 더하고 상품명은 바꾸지 않는다")
    void inboundRegisteredSkuAddsQuantity() {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 10));

        StockStatus result = inventoryService.inbound(new InboundItem(sku, "사이다", 5));

        assertThat(result.product().getName()).isEqualTo("콜라");
        assertThat(result.stock().getQuantity()).isEqualTo(15);
        assertThat(inventoryService.getStockBySku(sku).stock().getQuantity()).isEqualTo(15);
        assertThat(movements(result.product().getId()))
                .containsExactly(new MovementRow("INBOUND", 10, 10), new MovementRow("INBOUND", 5, 15));
    }

    @Test
    @DisplayName("등록되지 않은 SKU의 첫 입고가 동시에 들어와도 상품은 한 번만 등록되고 입고는 모두 반영된다")
    void concurrentFirstInboundRegistersProductOnce() throws Exception {
        String sku = newSku();

        inboundConcurrently(sku, 50);

        // 상품이나 재고 행을 두 번 만들려 한 요청은 UNIQUE 제약에 걸려 실패한다. 50건이 모두 성공했으니 한 번씩만 만들었다
        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isEqualTo(50);
        assertThat(movements(status.product().getId()))
                .extracting(MovementRow::quantityAfter)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(1, 50));
    }

    @Test
    @DisplayName("등록된 상품에 입고가 동시에 들어와도 수량이 빠짐없이 늘어난다")
    void concurrentInboundAddsEveryQuantity() throws Exception {
        String sku = newSku();
        inventoryService.inbound(new InboundItem(sku, "콜라", 100));

        inboundConcurrently(sku, 100);

        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isEqualTo(200);
        // 준비 입고 1건(100)과 동시 입고 100건(101~200). 값이 겹치면 두 트랜잭션이 같은 수량을 보고 계산한 것이다
        assertThat(movements(status.product().getId()))
                .extracting(MovementRow::quantityAfter)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(100, 200));
    }

    /**
     * 스레드를 한꺼번에 출발시켜 같은 SKU로 1개씩 입고한다.
     * 한 건이라도 예외가 나면 Future.get()이 예외를 던져 테스트가 실패한다.
     */
    private void inboundConcurrently(String sku, int requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
            List<Future<StockStatus>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return inventoryService.inbound(new InboundItem(sku, "콜라", 1));
                }));
            }
            start.countDown();
            for (Future<StockStatus> future : futures) {
                future.get();
            }
        }
    }

    private List<MovementRow> movements(Long productId) {
        return jdbcTemplate.query("""
                        SELECT type, quantity, quantity_after
                        FROM product_stock_movement
                        WHERE product_id = ?
                        ORDER BY id
                        """,
                (rs, rowNum) -> new MovementRow(rs.getString("type"), rs.getInt("quantity"), rs.getInt("quantity_after")),
                productId);
    }

    private static String newSku() {
        return "SKU-" + UUID.randomUUID();
    }

    private static List<Integer> rangeClosed(int from, int to) {
        return IntStream.rangeClosed(from, to).boxed().toList();
    }

    private record MovementRow(String type, int quantity, int quantityAfter) {
    }
}
