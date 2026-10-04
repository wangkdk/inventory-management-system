package inventory.domain.stock;

public class InsufficientStockException extends RuntimeException {

    private final int available;
    private final int requested;

    public InsufficientStockException(Long productId, int available, int requested) {
        super("재고가 부족합니다. productId=" + productId + ", available=" + available + ", requested=" + requested);
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
