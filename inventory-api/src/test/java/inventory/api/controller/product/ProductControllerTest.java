package inventory.api.controller.product;

import inventory.WebContextTest;
import inventory.api.config.error.ErrorCode;
import inventory.api.controller.product.dto.ProductStockResponse;
import inventory.domain.product.Product;
import inventory.domain.product.ProductNotFoundException;
import inventory.domain.stock.InventoryService;
import inventory.domain.stock.ProductStock;
import inventory.domain.stock.StockStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(ProductController.class)
@WebContextTest
class ProductControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("id로 상품과 현재 재고 수량을 조회한다")
    void getProductById() {
        when(inventoryService.getStock(1L)).thenReturn(stockStatus(1L, "SKU-001", "콜라", 7));

        assertThat(mvc.get().uri("/api/v1/products/{productId}", 1L))
                .hasStatusOk()
                .bodyJson()
                .convertTo(ProductStockResponse.class)
                .isEqualTo(new ProductStockResponse(1L, "SKU-001", "콜라", 7));
    }

    @Test
    @DisplayName("SKU로 상품과 현재 재고 수량을 조회한다")
    void getProductBySku() {
        when(inventoryService.getStockBySku("SKU-002")).thenReturn(stockStatus(2L, "SKU-002", "사이다", 3));

        assertThat(mvc.get().uri("/api/v1/products").param("sku", "SKU-002"))
                .hasStatusOk()
                .bodyJson()
                .convertTo(ProductStockResponse.class)
                .isEqualTo(new ProductStockResponse(2L, "SKU-002", "사이다", 3));
    }

    @Test
    @DisplayName("없는 상품을 id로 조회하면 404와 PRODUCT_NOT_FOUND를 돌려준다")
    void getProductReturnsNotFound() {
        when(inventoryService.getStock(999L)).thenThrow(ProductNotFoundException.byId(999L));

        assertThat(mvc.get().uri("/api/v1/products/{productId}", 999L))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    @DisplayName("없는 SKU로 조회하면 404와 PRODUCT_NOT_FOUND를 돌려준다")
    void getProductBySkuReturnsNotFound() {
        when(inventoryService.getStockBySku("SKU-999")).thenThrow(ProductNotFoundException.bySku("SKU-999"));

        assertThat(mvc.get().uri("/api/v1/products").param("sku", "SKU-999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    @DisplayName("SKU가 비어 있으면 서비스를 부르지 않고 400과 INVALID_REQUEST를 돌려준다")
    void getProductBySkuRejectsBlankSku() {
        assertThat(mvc.get().uri("/api/v1/products").param("sku", " "))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("INVALID_REQUEST");
        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("컨트롤러 검증을 통과한 요청에서 IllegalArgumentException이 나면 서버 버그로 보고 500과 INTERNAL_ERROR를 돌려준다")
    void illegalArgumentReturnsInternalError() {
        when(inventoryService.getStock(1L)).thenThrow(IllegalArgumentException.class);

        assertThat(mvc.get().uri("/api/v1/products/{productId}", 1L))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("id가 숫자가 아니면 400과 INVALID_REQUEST를 돌려준다")
    void getProductRejectsNonNumericId() {
        assertThat(mvc.get().uri("/api/v1/products/{productId}", "abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("INVALID_REQUEST");
    }

    @Test
    @DisplayName("에러 응답의 title은 코드마다 고정된 문구이고, 예외 메시지는 응답에 싣지 않는다")
    void errorResponseHidesExceptionMessage() {
        when(inventoryService.getStock(999L)).thenThrow(ProductNotFoundException.byId(999L));

        MvcTestResult result = mvc.get().uri("/api/v1/products/{productId}", 999L).exchange();

        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo(ErrorCode.PRODUCT_NOT_FOUND.getTitle());
        assertThat(result).bodyJson().doesNotHavePath("$.detail");
    }

    @Test
    @DisplayName("없는 경로를 요청하면 404와 NOT_FOUND를 돌려준다")
    void unknownPathReturnsNotFound() {
        assertThat(mvc.get().uri("/api/v1/unknown"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("지원하지 않는 메서드로 요청하면 405와 METHOD_NOT_ALLOWED를 돌려준다")
    void unsupportedMethodReturnsMethodNotAllowed() {
        assertThat(mvc.post().uri("/api/v1/products/{productId}", 1L))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("METHOD_NOT_ALLOWED");
    }

    private static StockStatus stockStatus(Long productId, String sku, String name, int quantity) {
        return new StockStatus(Product.of(productId, sku, name), ProductStock.of(productId, quantity));
    }
}
