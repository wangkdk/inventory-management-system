package inventory.storage.db.core.stock;

import inventory.domain.stock.IdempotencyKeyMismatchException;
import inventory.domain.stock.IdempotencyRecord;
import inventory.domain.stock.IdempotencyRecordRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class IdempotencyRecordRepositoryAdapter implements IdempotencyRecordRepository {

    private final IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    public IdempotencyRecordRepositoryAdapter(IdempotencyRecordJpaRepository idempotencyRecordJpaRepository) {
        this.idempotencyRecordJpaRepository = idempotencyRecordJpaRepository;
    }

    /**
     * 같은 키의 기록이 이미 있으면 IdempotencyKeyMismatchException으로 바꿔 던진다.
     * 같은 상품이면 재고 행 잠금 앞에서 차례를 기다려 먼저 기록을 찾으므로, 여기까지 오는 건 같은 키를 다른 상품에 동시에 쓴 경우다.
     * 다른 제약 위반(CHECK, 외래 키)은 서버 버그라 그대로 던진다.
     */
    @Override
    @Transactional
    public void save(IdempotencyRecord idempotencyRecord) {
        try {
            idempotencyRecordJpaRepository.save(IdempotencyRecordEntity.of(idempotencyRecord));
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException violation
                    && violation.getKind() == ConstraintKind.UNIQUE) {
                throw new IdempotencyKeyMismatchException(idempotencyRecord.getIdempotencyKey());
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        return idempotencyRecordJpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(IdempotencyRecordEntity::toIdempotencyRecord);
    }
}
