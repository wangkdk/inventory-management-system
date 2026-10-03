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

    public static ProductStock of(Long productId, int quantity) {
        return new ProductStock(productId, quantity);
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
