package inventory.domain.stock;

public class StockLimitExceededException extends RuntimeException {

    private final int current;
    private final int requested;
    private final int limit;

    public StockLimitExceededException(Long productId, int current, int requested, int limit) {
        super("재고 수량이 최대치를 넘습니다. productId=" + productId + ", current=" + current
                + ", requested=" + requested + ", limit=" + limit);
        this.current = current;
        this.requested = requested;
        this.limit = limit;
    }

    public int getCurrent() {
        return current;
    }

    public int getRequested() {
        return requested;
    }

    public int getLimit() {
        return limit;
    }
}
