-- 상품 마스터
CREATE TABLE IF NOT EXISTS product (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku        VARCHAR(64)  NOT NULL CONSTRAINT uk_product_sku UNIQUE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 재고: 상품마다 하나. 입고와 출고는 이 행을 잠그고 수량을 바꾼다
CREATE TABLE IF NOT EXISTS product_stock (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT      NOT NULL
               CONSTRAINT uk_product_stock_product UNIQUE
               CONSTRAINT fk_product_stock_product REFERENCES product (id),
    quantity   INTEGER     NOT NULL DEFAULT 0
               CONSTRAINT ck_product_stock_quantity_non_negative CHECK (quantity >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 입출고 기록: 한 줄이 입고나 출고 한 번이다. 추가만 하고 고치지 않는다
CREATE TABLE IF NOT EXISTS product_stock_movement (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id     BIGINT      NOT NULL
                   CONSTRAINT fk_product_stock_movement_product REFERENCES product (id),
    type           VARCHAR(16) NOT NULL
                   CONSTRAINT ck_product_stock_movement_type CHECK (type IN ('INBOUND', 'OUTBOUND')),
    quantity       INTEGER     NOT NULL
                   CONSTRAINT ck_product_stock_movement_quantity_positive CHECK (quantity > 0),
    quantity_after INTEGER     NOT NULL
                   CONSTRAINT ck_product_stock_movement_quantity_after_non_negative CHECK (quantity_after >= 0),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 요청 키 기록: 요청 키로 처리한 입고나 출고와 그 결과. 다시 보낼 수 있는 동안만 필요해서 입출고 기록과 따로 둔다
CREATE TABLE IF NOT EXISTS idempotency_record (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL
                    CONSTRAINT uk_idempotency_record_key UNIQUE,
    product_id      BIGINT      NOT NULL
                    CONSTRAINT fk_idempotency_record_product REFERENCES product (id),
    type            VARCHAR(16) NOT NULL
                    CONSTRAINT ck_idempotency_record_type CHECK (type IN ('INBOUND', 'OUTBOUND')),
    quantity        INTEGER     NOT NULL
                    CONSTRAINT ck_idempotency_record_quantity_positive CHECK (quantity > 0),
    quantity_after  INTEGER     NOT NULL
                    CONSTRAINT ck_idempotency_record_quantity_after_non_negative CHECK (quantity_after >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
