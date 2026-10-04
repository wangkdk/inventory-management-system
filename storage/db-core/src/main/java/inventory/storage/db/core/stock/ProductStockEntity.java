package inventory.storage.db.core.stock;

import inventory.domain.stock.ProductStock;
import inventory.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "product_stock",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_stock_product", columnNames = "product_id")
)
public class ProductStockEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    protected ProductStockEntity() {
    }

    public ProductStockEntity(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public static ProductStockEntity of(ProductStock stock) {
        return new ProductStockEntity(stock.getProductId(), stock.getQuantity());
    }

    public ProductStock toProductStock() {
        return ProductStock.of(productId, quantity);
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
