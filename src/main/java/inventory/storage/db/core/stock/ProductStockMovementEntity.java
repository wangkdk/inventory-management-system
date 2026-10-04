package inventory.storage.db.core.stock;

import inventory.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    /**
     * domain의 MovementType 이름(INBOUND, OUTBOUND)을 저장한다. 현재 구조에서 storage는 domain을 참조하지 않아서 문자열로 둔다.
     */
    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int quantityAfter;

    protected ProductStockMovementEntity() {
    }

    public ProductStockMovementEntity(Long productId, String type, int quantity, int quantityAfter) {
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }
}
