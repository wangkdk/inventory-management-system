package inventory.api.controller.inventory;

import inventory.WebContextTest;
import inventory.api.controller.inventory.dto.InboundRequest;
import inventory.api.controller.product.dto.ProductStockResponse;
import inventory.domain.product.Product;
import inventory.domain.stock.InboundItem;
import inventory.domain.stock.InventoryService;
import inventory.domain.stock.ProductStock;
import inventory.domain.stock.StockStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(InventoryController.class)
@WebContextTest
class InventoryControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("입고하면 200과 입고 뒤 재고를 돌려준다")
    void inboundReturnsStock() {
        when(inventoryService.inbound(new InboundItem("SKU-001", "콜라", 10)))
                .thenReturn(stockStatus(1L, "SKU-001", "콜라", 10));

        MvcTestResult result = inbound(new InboundRequest("SKU-001", "콜라", 10));

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(ProductStockResponse.class)
                .isEqualTo(new ProductStockResponse(1L, "SKU-001", "콜라", 10));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "' ', 콜라, 10",
            "SKU-001, ' ', 10",
            "SKU-001, 콜라, 0",
            "SKU-001, 콜라, -1",
            "SKU-001, 콜라, null"
    })
    @DisplayName("요청 값이 잘못되면 서비스를 부르지 않고 400과 INVALID_REQUEST를 돌려준다")
    void inboundRejectsInvalidRequest(String sku, String name, Integer quantity) {
        MvcTestResult result = inbound(new InboundRequest(sku, name, quantity));

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("INVALID_REQUEST");
        verifyNoInteractions(inventoryService);
    }

    private MvcTestResult inbound(InboundRequest request) {
        return mvc.post().uri("/api/v1/inventory/inbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .exchange();
    }

    private static StockStatus stockStatus(Long productId, String sku, String name, int quantity) {
        return new StockStatus(Product.of(productId, sku, name), ProductStock.of(productId, quantity));
    }
}
