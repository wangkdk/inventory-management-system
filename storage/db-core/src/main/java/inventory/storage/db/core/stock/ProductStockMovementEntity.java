package inventory.storage.db.core.stock;

import inventory.domain.stock.MovementType;
import inventory.domain.stock.ProductStockMovement;
import inventory.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_stock_movement")
public class ProductStockMovementEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int quantityAfter;

    protected ProductStockMovementEntity() {
    }

    public ProductStockMovementEntity(Long productId, MovementType type, int quantity, int quantityAfter) {
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
    }

    public static ProductStockMovementEntity of(ProductStockMovement movement) {
        return new ProductStockMovementEntity(
                movement.getProductId(), movement.getType(), movement.getQuantity(), movement.getQuantityAfter());
    }

    public Long getId() {
        return id;
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
