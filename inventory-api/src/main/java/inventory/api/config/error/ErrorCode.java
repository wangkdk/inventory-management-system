package inventory.api.config.error;

import org.springframework.http.HttpStatus;

/**
 * 에러 응답의 code와 title. title은 고객에게 그대로 보여도 되는 고정 문구다.
 */
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다"),
    STOCK_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "재고 수량이 최대치를 넘습니다"),
    STOCK_LOCK_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "요청이 몰려 처리하지 못했습니다. 잠시 후 다시 시도해 주세요"),
    IDEMPOTENCY_KEY_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "같은 요청 키로 다른 요청을 보냈습니다");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}
