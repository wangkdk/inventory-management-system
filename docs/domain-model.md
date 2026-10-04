# 재고 관리 시스템 도메인 모델

## 재고 관리 도메인

- 재고 관리 시스템은 상품별 현재 재고 수량을 관리한다.
- 현장에서는 SKU로 상품을 식별한다.
- 입고는 재고 수량을 늘린다.
    - 등록되지 않은 SKU로 입고하면 상품을 먼저 등록한 뒤 입고한다.
- 출고는 재고 수량을 줄인다.
    - 재고는 음수가 될 수 없으므로 재고보다 많이 출고할 수 없다.
- 상품별 현재 재고 수량을 조회할 수 있다.
- 같은 상품에 입고와 출고가 동시에 들어올 수 있다. 동시에 들어와도 모든 변경이 빠짐없이 반영되어야 한다.
- 모든 입고와 출고는 기록으로 남는다.

## 도메인 모델

- `domain.product`: 상품 애그리거트
- `domain.stock`: 재고 애그리거트, 입출고 기록 애그리거트

---

### [상품 애그리거트]

### 상품 (Product)

_Aggregate Root_

#### 속성

- `id`: `Long` 내부 식별자. 재고와 입출고 기록이 상품을 가리킬 때 쓴다
- `sku`: 상품 코드(SKU). 현장에서 상품을 식별하는 값이고, 입고 출고 요청은 SKU로 상품을 찾는다
- `name`: 상품명

#### 행위

- `static register()`: 상품 등록: `sku`, `name`

#### 규칙

- SKU는 중복을 허용하지 않는다
- SKU는 등록한 뒤 바꿀 수 없다
- SKU는 비어 있을 수 없다
- SKU는 앞뒤 공백을 제거해서 다루고, 대소문자는 구분한다
- 상품명은 비어 있을 수 없다

### 상품 없음 (ProductNotFoundException)

_Exception_

- SKU나 id로 상품을 찾지 못했을 때 던진다

### 상품 등록 결과 (ProductRegistration)

_Value Object_

#### 속성

- `product`: `Product` 등록된 상품. 이미 등록된 SKU였으면 기존 상품이다
- `newlyRegistered`: 이번 요청으로 새로 등록했는지 여부

### [재고 애그리거트]

### 재고 (ProductStock)

_Aggregate Root_

#### 속성

- `productId`: `Long` 상품. 재고의 식별자이고, 상품 하나에 재고가 하나 있다
- `quantity`: `int` 현재 재고 수량

#### 행위

- `static create()`: 수량이 0인 재고를 만든다: `productId`
- `inbound()`: 재고 수량을 늘린다: `quantity`
- `outbound()`: 재고 수량을 줄인다: `quantity`

#### 규칙

- 상품은 `productId`로만 참조한다. 상품 객체를 직접 들고 있지 않는다
- 재고 수량은 음수가 될 수 없다
- 입고 수량과 출고 수량은 1 이상이다
- 재고보다 많이 출고하려 하면 `InsufficientStockException`을 던진다
- 재고 수량은 `inbound()`와 `outbound()`로만 바꾼다
- 입출고 기록을 목록으로 갖지 않는다. 기록은 따로 저장한다

### 재고 부족 (InsufficientStockException)

_Exception_

- 재고보다 많이 출고하려 할 때 던진다. 남은 수량과 요청 수량을 담는다

### 재고 현황 (StockStatus)

_Value Object_

#### 속성

- `product`: `Product` 상품
- `stock`: `ProductStock` 그 상품의 현재 재고

### 입고 품목 (InboundItem)

_Value Object_

#### 속성

- `sku`: 입고할 상품의 SKU
- `name`: 상품명. 등록되지 않은 SKU를 등록할 때만 쓴다
- `quantity`: `int` 입고 수량

### [입출고 기록 애그리거트]

### 입출고 기록 (ProductStockMovement)

_Aggregate Root_

#### 속성

- `productId`: `Long` 상품
- `type`: `MovementType` 입출고 유형
- `quantity`: `int` 변동 수량
- `quantityAfter`: `int` 변동 뒤 재고 수량

#### 행위

- `static record()`: 입출고를 기록한다: `productId`, `type`, `quantity`, `quantityAfter`

#### 규칙

- 상품은 `productId`로만 참조한다
- 변동 수량은 1 이상이고, 변동 뒤 재고 수량은 0 이상이다
- 추가만 하고 고치거나 지우지 않는다

### 입출고 유형 (MovementType)

_Enum_

#### 상수

- `INBOUND`: 입고
- `OUTBOUND`: 출고

## 유스케이스

- 같은 재고에 입고와 출고가 동시에 들어와도 하나씩 차례로 반영된다. 어떤 변경도 사라지지 않는다
- 재고 수량 변경과 입출고 기록은 함께 반영된다. 둘 중 하나만 반영되는 일은 없다

### 입고

- 등록되지 않은 SKU면 상품을 등록하고, 수량이 0인 재고를 함께 만든다
- 등록되지 않은 SKU의 첫 입고가 동시에 들어와도 상품과 재고는 하나씩만 생긴다
- 이미 등록된 SKU면 요청의 상품명은 쓰지 않는다. 입고는 상품 정보를 바꾸지 않는다
- 재고 수량을 늘리고 입출고 기록을 남긴다

### 출고

- SKU로 상품을 찾는다. 없으면 `ProductNotFoundException`을 던진다
- 재고 수량을 줄이고 입출고 기록을 남긴다
- 재고가 모자라면 일부만 출고하지 않고 요청 전체를 거절한다

### 재고 조회

- 상품의 현재 재고 수량을 id나 SKU로 조회한다. 상품이 없으면 `ProductNotFoundException`을 던진다
- SKU로 조회할 때도 등록할 때와 같은 규칙으로 앞뒤 공백을 제거한다
