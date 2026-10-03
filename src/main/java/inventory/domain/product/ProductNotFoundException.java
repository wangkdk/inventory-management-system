package inventory.domain.product;

public class ProductNotFoundException extends RuntimeException {

    private ProductNotFoundException(String message) {
        super(message);
    }

    public static ProductNotFoundException byId(Long id) {
        return new ProductNotFoundException("상품을 찾을 수 없습니다. id=" + id);
    }

    public static ProductNotFoundException bySku(String sku) {
        return new ProductNotFoundException("상품을 찾을 수 없습니다. sku=" + sku);
    }
}
