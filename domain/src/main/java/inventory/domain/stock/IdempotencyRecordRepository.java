package inventory.domain.stock;

import java.util.Optional;

public interface IdempotencyRecordRepository {

    void save(IdempotencyRecord idempotencyRecord);

    /**
     * 요청 키로 남긴 기록을 찾는다. 재고 행을 잠근 뒤에 불러야, 같은 키로 동시에 다시 보낸 요청이 한 번만 반영된다.
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
