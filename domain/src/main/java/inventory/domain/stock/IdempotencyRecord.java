package inventory.domain.stock;

public class IdempotencyRecord {

    private final String idempotencyKey;
    private final Long productId;
    private final MovementType type;
    private final int quantity;
    private final int quantityAfter;

    private IdempotencyRecord(
            String idempotencyKey, Long productId, MovementType type, int quantity, int quantityAfter) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("요청 키는 비어 있을 수 없습니다");
        }
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
        this.idempotencyKey = idempotencyKey;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
    }

    public static IdempotencyRecord of(
            String idempotencyKey, Long productId, MovementType type, int quantity, int quantityAfter) {
        return new IdempotencyRecord(idempotencyKey, productId, type, quantity, quantityAfter);
    }

    /**
     * 같은 요청 키로 다시 들어온 요청이 이 기록을 남긴 요청과 같은 내용인지 확인한다.
     */
    public boolean isSameRequest(Long productId, MovementType type, int quantity) {
        return this.productId.equals(productId) && this.type == type && this.quantity == quantity;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
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
