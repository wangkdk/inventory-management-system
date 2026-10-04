package inventory.domain.stock;

/**
 * 재고 행 잠금을 정해진 시간 안에 얻지 못했을 때 던진다. 트랜잭션이 롤백되므로 요청은 하나도 반영되지 않는다.
 */
public class StockLockTimeoutException extends RuntimeException {

    public StockLockTimeoutException(Long productId, Throwable cause) {
        super("재고 잠금을 기다리다 시간이 초과됐습니다. productId=" + productId, cause);
    }
}
