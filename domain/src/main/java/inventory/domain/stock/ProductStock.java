package inventory.domain.stock;

public class ProductStock {

    private final Long productId;
    private final int quantity;

    private ProductStock(Long productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 id는 비어 있을 수 없습니다");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("재고 수량은 음수가 될 수 없습니다: " + quantity);
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    public static ProductStock create(Long productId) {
        return new ProductStock(productId, 0);
    }

    public static ProductStock of(Long productId, int quantity) {
        return new ProductStock(productId, quantity);
    }

    public ProductStock inbound(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다: " + quantity);
        }
        return new ProductStock(productId, Math.addExact(this.quantity, quantity));
    }

    public ProductStock outbound(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("출고 수량은 1 이상이어야 합니다: " + quantity);
        }
        if (quantity > this.quantity) {
            throw new InsufficientStockException(productId, this.quantity, quantity);
        }
        return new ProductStock(productId, this.quantity - quantity);
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
