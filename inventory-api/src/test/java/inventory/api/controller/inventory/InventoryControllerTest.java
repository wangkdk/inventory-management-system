package inventory.api.controller.inventory;

import inventory.WebContextTest;
import inventory.api.controller.inventory.dto.InboundRequest;
import inventory.api.controller.inventory.dto.OutboundRequest;
import inventory.api.controller.product.dto.ProductStockResponse;
import inventory.domain.product.Product;
import inventory.domain.product.ProductNotFoundException;
import inventory.domain.stock.InboundItem;
import inventory.domain.stock.InsufficientStockException;
import inventory.domain.stock.InventoryService;
import inventory.domain.stock.OutboundItem;
import inventory.domain.stock.ProductStock;
import inventory.domain.stock.StockLimitExceededException;
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
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

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
                .apply(document("inventory-inbound",
                        requestFields(
                                fieldWithPath("sku").description("SKU. 64자 이하"),
                                fieldWithPath("name").description("상품명. 200자 이하. 등록되지 않은 SKU를 등록할 때만 쓴다"),
                                fieldWithPath("quantity").description("입고 수량. 1 이상")),
                        responseFields(
                                fieldWithPath("id").description("상품 id"),
                                fieldWithPath("sku").description("SKU"),
                                fieldWithPath("name").description("저장된 상품명"),
                                fieldWithPath("quantity").description("입고 뒤 재고 수량"))))
                .bodyJson()
                .convertTo(ProductStockResponse.class)
                .isEqualTo(new ProductStockResponse(1L, "SKU-001", "콜라", 10));
    }

    @Test
    @DisplayName("입고 뒤 수량이 최대치를 넘으면 409와 STOCK_LIMIT_EXCEEDED, 현재 수량과 요청 수량과 최대치를 돌려준다")
    void inboundBeyondLimitReturnsConflict() {
        when(inventoryService.inbound(new InboundItem("SKU-001", "콜라", 10)))
                .thenThrow(new StockLimitExceededException(1L, Integer.MAX_VALUE - 5, 10, Integer.MAX_VALUE));

        MvcTestResult result = inbound(new InboundRequest("SKU-001", "콜라", 10));

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .apply(document("error-stock-limit-exceeded",
                        relaxedResponseFields(
                                fieldWithPath("current").description("현재 재고 수량"),
                                fieldWithPath("requested").description("요청한 입고 수량"),
                                fieldWithPath("limit").description("재고 수량의 최대치"))));
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("STOCK_LIMIT_EXCEEDED");
        assertThat(result).bodyJson().extractingPath("$.current").isEqualTo(Integer.MAX_VALUE - 5);
        assertThat(result).bodyJson().extractingPath("$.requested").isEqualTo(10);
        assertThat(result).bodyJson().extractingPath("$.limit").isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("입고 수량이 int 범위를 넘으면 서비스를 부르지 않고 400과 INVALID_REQUEST를 돌려준다")
    void inboundRejectsQuantityBeyondInt() {
        MvcTestResult result = mvc.post().uri("/api/v1/inventory/inbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sku": "SKU-001", "name": "콜라", "quantity": 3000000000}
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("INVALID_REQUEST");
        verifyNoInteractions(inventoryService);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "' ', 콜라, 10",
            "SKU-001, ' ', 10",
            "SKU-001, 콜라, 0",
            "SKU-001, 콜라, -1",
            "SKU-001, 콜라, null"
    })
    @DisplayName("입고 요청 값이 잘못되면 서비스를 부르지 않고 400과 INVALID_REQUEST를 돌려준다")
    void inboundRejectsInvalidRequest(String sku, String name, Integer quantity) {
        MvcTestResult result = inbound(new InboundRequest(sku, name, quantity));

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("INVALID_REQUEST");
        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("출고하면 200과 출고 뒤 재고를 돌려준다")
    void outboundReturnsStock() {
        when(inventoryService.outbound(new OutboundItem("SKU-001", 3)))
                .thenReturn(stockStatus(1L, "SKU-001", "콜라", 7));

        MvcTestResult result = outbound(new OutboundRequest("SKU-001", 3));

        assertThat(result)
                .hasStatusOk()
                .apply(document("inventory-outbound",
                        requestFields(
                                fieldWithPath("sku").description("SKU. 64자 이하"),
                                fieldWithPath("quantity").description("출고 수량. 1 이상")),
                        responseFields(
                                fieldWithPath("id").description("상품 id"),
                                fieldWithPath("sku").description("SKU"),
                                fieldWithPath("name").description("상품명"),
                                fieldWithPath("quantity").description("출고 뒤 재고 수량"))))
                .bodyJson()
                .convertTo(ProductStockResponse.class)
                .isEqualTo(new ProductStockResponse(1L, "SKU-001", "콜라", 7));
    }

    @Test
    @DisplayName("재고보다 많이 출고하면 409와 INSUFFICIENT_STOCK, 남은 수량과 요청 수량을 돌려준다")
    void outboundInsufficientStockReturnsConflict() {
        when(inventoryService.outbound(new OutboundItem("SKU-001", 5)))
                .thenThrow(new InsufficientStockException(1L, 2, 5));

        MvcTestResult result = outbound(new OutboundRequest("SKU-001", 5));

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .apply(document("error-insufficient-stock",
                        relaxedResponseFields(
                                fieldWithPath("available").description("출고할 수 있는 수량. 현재 재고 수량과 같다"),
                                fieldWithPath("requested").description("요청한 출고 수량"))));
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INSUFFICIENT_STOCK");
        assertThat(result).bodyJson().extractingPath("$.available").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.requested").isEqualTo(5);
    }

    @Test
    @DisplayName("없는 SKU를 출고하면 404와 PRODUCT_NOT_FOUND를 돌려준다")
    void outboundUnknownSkuReturnsNotFound() {
        when(inventoryService.outbound(new OutboundItem("SKU-404", 1)))
                .thenThrow(ProductNotFoundException.bySku("SKU-404"));

        MvcTestResult result = outbound(new OutboundRequest("SKU-404", 1));

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "' ', 3",
            "SKU-001, 0",
            "SKU-001, -1",
            "SKU-001, null"
    })
    @DisplayName("출고 요청 값이 잘못되면 서비스를 부르지 않고 400과 INVALID_REQUEST를 돌려준다")
    void outboundRejectsInvalidRequest(String sku, Integer quantity) {
        MvcTestResult result = outbound(new OutboundRequest(sku, quantity));

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

    private MvcTestResult outbound(OutboundRequest request) {
        return mvc.post().uri("/api/v1/inventory/outbound")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .exchange();
    }

    private static StockStatus stockStatus(Long productId, String sku, String name, int quantity) {
        return new StockStatus(Product.of(productId, sku, name), ProductStock.of(productId, quantity));
    }
}
