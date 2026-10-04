package inventory.domain.stock;

public class IdempotencyKeyMismatchException extends RuntimeException {

    public IdempotencyKeyMismatchException(String idempotencyKey) {
        super("이미 처리한 요청 키로 다른 요청이 들어왔습니다. idempotencyKey=" + idempotencyKey);
    }
}
