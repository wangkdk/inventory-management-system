package inventory.storage.db.core.stock;

import inventory.domain.stock.IdempotencyRecord;
import inventory.domain.stock.MovementType;
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
@Table(name = "idempotency_record")
public class IdempotencyRecordEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MovementType type;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int quantityAfter;

    protected IdempotencyRecordEntity() {
    }

    public IdempotencyRecordEntity(
            String idempotencyKey, Long productId, MovementType type, int quantity, int quantityAfter) {
        this.idempotencyKey = idempotencyKey;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
    }

    public static IdempotencyRecordEntity of(IdempotencyRecord idempotencyRecord) {
        return new IdempotencyRecordEntity(idempotencyRecord.getIdempotencyKey(), idempotencyRecord.getProductId(),
                idempotencyRecord.getType(), idempotencyRecord.getQuantity(), idempotencyRecord.getQuantityAfter());
    }

    public IdempotencyRecord toIdempotencyRecord() {
        return IdempotencyRecord.of(idempotencyKey, productId, type, quantity, quantityAfter);
    }

    public Long getId() {
        return id;
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
