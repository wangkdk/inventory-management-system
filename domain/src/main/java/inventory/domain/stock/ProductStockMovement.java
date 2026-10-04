package inventory.domain.stock;

public class ProductStockMovement {

    private final Long productId;
    private final MovementType type;
    private final int quantity;
    private final int quantityAfter;

    private ProductStockMovement(Long productId, MovementType type, int quantity, int quantityAfter) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 id는 비어 있을 수 없습니다");
        }
        if (type == null) {
            throw new IllegalArgumentException("입출고 유형은 비어 있을 수 없습니다");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("변동 수량은 1 이상이어야 합니다: " + quantity);
        }
        if (quantityAfter < 0) {
            throw new IllegalArgumentException("변동 뒤 재고 수량은 음수가 될 수 없습니다: " + quantityAfter);
        }
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
    }

    public static ProductStockMovement record(Long productId, MovementType type, int quantity, int quantityAfter) {
        return new ProductStockMovement(productId, type, quantity, quantityAfter);
    }

    public Long getProductId() {
        return productId;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }
}
