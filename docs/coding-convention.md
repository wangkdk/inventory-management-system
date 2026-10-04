# 코딩 컨벤션

## 의존 방향

순환 참조가 생기지 않도록 의존은 한 방향으로만 흐르게 한다.

### 모듈과 패키지

- 의존은 `inventory-api -> domain <- storage:db-core` 방향으로만 흐른다. domain은 다른 모듈을 참조하지 않는다
- 저장소 인터페이스는 domain에 `XxxRepository`로 둔다. storage는 이를 `XxxRepositoryAdapter`로 구현해 Spring Data 리포지토리에 이어 준다. 엔티티와 도메인 객체의 변환은 storage의 엔티티가 맡는다
- Spring Data 리포지토리는 `XxxJpaRepository`로 두고 storage 밖에서 쓰지 않는다
- inventory-api는 storage를 import하지 않는다. storage는 `runtimeOnly`로 걸려 있어서 import하면 컴파일 에러가 난다
- domain 안에서는 재고(`stock`)가 상품(`product`)을 참조하고, 상품은 재고를 참조하지 않는다
- 두 도메인에 걸친 유스케이스는 다른 쪽을 참조할 수 있는 도메인에 둔다. 상품과 재고 수량을 함께 조회하는 일은 재고 쪽 `InventoryService`가 맡는다

### 서비스와 컴포넌트

- `@Service`는 컨트롤러가 부르는 유스케이스 단위다
- 서비스는 다른 서비스를 부르지 않는다. 여러 서비스가 함께 쓰는 일은 `@Component`로 만들고, 서비스는 컴포넌트를 부른다
- 컴포넌트끼리는 불러도 된다

예를 들어 `InventoryService`는 상품을 찾을 때 `ProductService`가 아니라 `ProductFinder`를 부른다. 나중에 상품만 다루는 기능이 생겨 `ProductService`를 만들어도, 그 서비스 역시 `ProductFinder`를 부른다. 두 서비스는 서로를 부르지 않는다.

## 트랜잭션

트랜잭션은 최대한 짧게 가져간다. DB 연결과 잠금을 쥐고 있는 시간만큼 다른 요청이 기다린다.

- 저장소 구현이 메서드마다 트랜잭션을 건다. 조회는 `@Transactional(readOnly = true)`, 쓰기는 `@Transactional`
- 잠금에 기대는 저장소 메서드(잠금 조회, 잠근 행의 수정)만 `@Transactional(propagation = Propagation.MANDATORY)`로 건다. 서비스 트랜잭션 없이 부르면 예외가 난다
- 서비스에는 여러 저장소 호출을 한 트랜잭션으로 묶어야 할 때만 `@Transactional`을 둔다. 저장소의 트랜잭션은 거기에 합류한다
- 컴포넌트에는 `@Transactional`을 두지 않는다

예를 들어 `InventoryService.inbound`는 상품 등록, 재고 행 잠금, 수량 변경, 기록이 함께 커밋되거나 함께 롤백돼야 해서 서비스에 트랜잭션을 둔다. 재고 조회(`getStockBySku`)는 묶을 이유가 없어서 두지 않고, 저장소의 조회마다 짧은 트랜잭션으로 돈다.
