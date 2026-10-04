package inventory.api.config.error;

import inventory.domain.product.ProductNotFoundException;
import inventory.domain.stock.InsufficientStockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 에러 응답을 ProblemDetail(RFC 9457)로 보낸다. code와 title은 ErrorCode에서 가져오고,
 * 예외 메시지는 응답에 싣지 않고 로그에만 남긴다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException e) {
        log.warn("상품 없음: {}", e.getMessage());
        return problem(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException e) {
        log.warn("재고 부족: {}", e.getMessage());
        ProblemDetail problem = problem(ErrorCode.INSUFFICIENT_STOCK);
        problem.setProperty("available", e.getAvailable());
        problem.setProperty("requested", e.getRequested());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        log.debug("잘못된 요청: {}", e.getMessage());
        return problem(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return problem(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 스프링이 직접 처리하는 오류에도 상태에 맞는 code와 title을 붙인다.
     * 4xx의 detail("Required parameter 'sku' is not present." 등)은 요청을 고치는 데 쓸모 있어서 그대로 두고,
     * 5xx의 detail은 숨기고 로그에 남긴다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            ErrorCode code = errorCodeOf(statusCode);
            problem.setTitle(code.getTitle());
            problem.setProperty("code", code.name());
            if (statusCode.is5xxServerError()) {
                log.error("스프링이 처리한 서버 오류", ex);
                problem.setDetail(null);
            }
        }
        return response;
    }

    private static ErrorCode errorCodeOf(HttpStatusCode statusCode) {
        if (statusCode.is5xxServerError()) {
            return ErrorCode.INTERNAL_ERROR;
        }
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.NOT_FOUND;
        }
        if (statusCode.value() == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        return ErrorCode.INVALID_REQUEST;
    }

    private static ProblemDetail problem(ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatus(code.getStatus());
        problem.setTitle(code.getTitle());
        problem.setProperty("code", code.name());
        return problem;
    }
}
