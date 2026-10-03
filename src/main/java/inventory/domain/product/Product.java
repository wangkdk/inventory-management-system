package inventory.domain.product;

public class Product {

    private final Long id;
    private final String sku;
    private final String name;

    private Product(Long id, String sku, String name) {
        this.id = id;
        this.sku = normalizeSku(sku);
        this.name = name;
        validateName(this.name);
    }

    public static Product register(String sku, String name) {
        return new Product(null, sku, name);
    }

    public static Product of(Long id, String sku, String name) {
        return new Product(id, sku, name);
    }

    /**
     * 같은 SKU가 같은 문자열이 되도록 맞춘다. 유니크 제약은 문자열을 그대로 비교해서, 공백 하나 차이로 상품이 둘 생길 수 있다.
     * 앞뒤 공백은 지운다(전각 공백까지 지우려고 trim이 아니라 strip).
     * 대소문자는 바꾸지 않는다. SKU를 받는 모든 경로가 이 메서드를 거친다.
     *
     * @throws IllegalArgumentException SKU가 null이거나 공백뿐일 때
     */
    static String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU는 비어 있을 수 없습니다");
        }
        return sku.strip();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 비어 있을 수 없습니다");
        }
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }
}
