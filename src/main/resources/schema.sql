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
