# 테스트 가이드

## 단위 테스트

클래스 하나의 동작을 스프링 컨텍스트와 DB 없이 검증한다. Docker 없이 돌고, 가장 빠르다.

### 대상

스프링 컨텍스트와 DB 없이 입력과 출력만으로 동작을 확인할 수 있는 클래스는 모두 대상이다.

- 도메인 객체 (`Product` 등): [도메인 모델 문서](domain-model.md)에서 도메인 객체가 스스로 지키는 규칙마다 테스트를 최소 하나 둔다. 규칙을 바꾸면 테스트도 같이 바꾼다
- 그 밖의 클래스: 요청 DTO의 검증 애너테이션(`@NotBlank`, `@Size`), 도메인 객체를 응답 DTO로 바꾸는 변환 등
- DB가 지키는 규칙(SKU 중복 금지 등)은 단위 테스트가 아니라 저장소 테스트가 맡는다

### 작성 규칙

- 테스트 클래스는 대상과 같은 패키지에 `XxxTest`로 둔다
- 메서드 이름은 영어로 행위와 결과를 쓰고(`registerStripsSku`), `@DisplayName`은 규칙을 한글 문장으로 쓴다("상품을 등록하면 SKU의 앞뒤 공백을 제거한다")
- 실행과 검증 사이는 빈 줄로 나눈다
- 검증은 AssertJ로 한다. 값은 `assertThat`, 예외는 `assertThatThrownBy`
- 예외는 타입만 검증하고 메시지는 검증하지 않는다(`hasMessage`를 쓰지 않는다). 메시지 문구는 바뀔 수 있다
- 입력만 다르고 규칙이 같으면 `@ParameterizedTest`로 묶는다
    - 빈 값은 `@NullAndEmptySource`(null, 빈 문자열)와 `@ValueSource`(공백만 있는 문자열)로 함께 확인한다
- 스프링 컨텍스트, mock, DB를 쓰지 않는다. 협력 객체를 불러야 동작하는 서비스는 서비스 테스트를 따른다

### 예시

```java
@Test
@DisplayName("상품을 등록하면 SKU의 앞뒤 공백을 제거한다")
void registerStripsSku() {
    Product product = Product.register("  SKU-001 ", "콜라");

    assertThat(product.getSku()).isEqualTo("SKU-001");
}
```

## API 테스트 (`WebMvcTest`)

컨트롤러와 에러 처리기만 띄우고, 서비스는 mock으로 바꿔서 웹 계층을 검증한다. Docker 없이 돈다.

### 대상

- 경로, 경로 변수, 쿼리 파라미터, 요청 본문이 서비스 호출로 바뀌는지
- 응답 JSON의 모양과 상태 코드
- 요청 값 검증(`@NotBlank`, `@Valid`)에 걸리면 400을 돌려주는지
- 에러 응답의 상태, `code`, `title`. 스프링이 처리하는 오류(타입 불일치, 없는 경로, 지원하지 않는 메서드)도 포함한다

### 작성 규칙

- 테스트 클래스에 `@WebMvcTest(XxxController.class)`와 `@WebContextTest`를 함께 붙인다. `@WebContextTest`가 태그(`web-context`)를 건다
- 서비스는 `@MockitoBean`으로 바꾸고, 시나리오에 필요한 반환값이나 예외만 `when(...)`으로 정한다
- 요청과 검증은 `MockMvcTester`로 한다
- 정상 응답은 응답 DTO로 바꿔 통째로 비교한다(`bodyJson().convertTo(...)`)
- 에러 응답은 상태와 `code`로 검증한다. `title`을 검증할 때는 문자열을 적지 않고 `ErrorCode` 상수와 비교한다
- 비즈니스 규칙과 DB 동작은 검증하지 않는다. 단위 테스트와 저장소 테스트가 맡는다
- 이름, `@DisplayName`, 실행과 검증 사이 빈 줄 규칙은 단위 테스트와 같다

### 예시

```java
@WebMvcTest(ProductController.class)
@WebContextTest
class ProductControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("없는 상품을 id로 조회하면 404와 PRODUCT_NOT_FOUND를 돌려준다")
    void getProductReturnsNotFound() {
        when(inventoryService.getStock(999L)).thenThrow(ProductNotFoundException.byId(999L));

        assertThat(mvc.get().uri("/api/v1/products/{productId}", 999L))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }
}
```

## 서비스 테스트 (`Mockito`)

서비스가 부르는 컴포넌트와 리포지토리를 Mockito mock으로 바꾸고, 서비스의 분기와 흐름을 검증한다. 스프링 컨텍스트와 DB 없이 돈다. 서비스는 이 방식으로 테스트하는 것이 기본이다.

### 대상

- 조건에 따라 무엇을 하고 무엇을 하지 않는지: 이번에 새로 등록한 상품이면 재고 행을 만들고, 이미 있던 상품이면 만들지 않는다
- 예외가 나는 경로: 재고가 모자라면 수량을 바꾸지 않고 기록도 남기지 않는다
- DB가 결과를 정하는 동작(잠금, 트랜잭션, 제약)은 여기서 검증하지 않는다. mock은 테스트가 정한 값을 돌려줄 뿐이라서 DB가 실제로 어떻게 동작하는지 알 수 없다. 이런 동작은 DB와 함께 도는 서비스 테스트가 맡는다

### 작성 규칙

- 테스트 클래스는 `XxxServiceTest`로 두고 `@ExtendWith(MockitoExtension.class)`를 붙인다
- 협력 객체는 `@Mock`으로, 테스트할 서비스는 `@InjectMocks`로 만든다
- stub(`when`)은 시나리오에 필요한 것만 둔다. `MockitoExtension`은 쓰지 않은 stub이 있으면 테스트를 실패시킨다
- 결과로 확인할 수 있으면 결과로 검증한다. `verify`는 저장처럼 결과에 드러나지 않는 일을 확인할 때만 쓴다
- 이름, `@DisplayName`, 실행과 검증 사이 빈 줄, 검증 규칙은 단위 테스트와 같다

### 예시

```java
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductFinder productFinder;

    @Mock
    private ProductRegistrar productRegistrar;

    @Mock
    private ProductStockJpaRepository productStockJpaRepository;

    @Mock
    private ProductStockMovementJpaRepository productStockMovementJpaRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("재고가 모자라면 수량을 바꾸지 않고 기록도 남기지 않는다")
    void outboundWithInsufficientStockChangesNothing() {
        when(productFinder.getProductBySku("SKU-001")).thenReturn(Product.of(1L, "SKU-001", "콜라"));
        ProductStockEntity stockEntity = new ProductStockEntity(1L, 2);
        when(productStockJpaRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(stockEntity));

        assertThatThrownBy(() -> inventoryService.outbound(new OutboundItem("SKU-001", 5)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(stockEntity.getQuantity()).isEqualTo(2);
        verify(productStockMovementJpaRepository, never()).save(any());
    }
}
```

## 저장소 테스트 (`DataJpaTest`)

JPA에 필요한 빈만 띄우고, 실제 PostgreSQL 컨테이너로 검증한다. Docker가 있어야 돈다.

### 대상

- Spring Data 리포지토리의 쿼리 메서드와 네이티브 쿼리(`ON CONFLICT` 등)
- DB가 지키는 규칙: SKU 중복 금지 같은 제약
- 엔티티 매핑이 schema.sql과 맞는지

### 작성 규칙

- 테스트 클래스에 `@DataJpaTest`와 `@DbContextTest`를 함께 붙인다. `@DbContextTest`가 태그(`db-context`), test 프로필, Testcontainers 설정을 건다
- `@DataJpaTest`는 엔티티와 Spring Data 리포지토리만 띄운다. `@Service`나 직접 만든 `@Repository` 클래스는 빈으로 올라오지 않는다. 서비스는 서비스 테스트에서 검증한다
- 테스트마다 트랜잭션이 걸리고 끝나면 롤백된다. 테스트끼리 데이터를 지우지 않아도 된다
- 한 트랜잭션 안에서 도는 테스트라서 동시성은 여기서 검증하지 않는다. 동시성은 DB와 함께 도는 서비스 테스트에서 검증한다
- DB는 PostgreSQL만 쓴다. `FOR UPDATE`와 `ON CONFLICT`가 H2에서는 다르게 동작하므로 H2는 쓰지 않는다
- 테스트 설정은 `src/test/resources/application-test.yaml`에 둔다.
- 이름, `@DisplayName`, 검증, 예외 메시지 규칙은 단위 테스트와 같다

### 예시

```java
@DataJpaTest
@DbContextTest
class ProductJpaRepositoryTest {

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Test
    @DisplayName("이미 등록된 SKU면 넣지 않고 기존 상품명을 유지한다")
    void insertIfAbsentIgnoresRegisteredSku() {
        productJpaRepository.insertIfAbsent("SKU-001", "콜라");

        int inserted = productJpaRepository.insertIfAbsent("SKU-001", "사이다");

        assertThat(inserted).isZero();
    }
}
```

## DB와 함께 도는 서비스 테스트

서비스 테스트의 예외다. mock으로는 검증할 수 없는, DB가 결과를 정하는 동작만 이 방식으로 검증한다. JPA와 테스트할 서비스, 그 서비스가 쓰는 컴포넌트만 띄우고 실제 PostgreSQL 컨테이너에서 서비스를 부른다. Docker가 있어야 돈다.

### 언제 쓰나

- 잠금과 동시성: 같은 행을 두고 여러 트랜잭션이 다퉈도 변경이 빠짐없이 반영되는지 (동시 입고, 동시 출고)
- DB 제약에 기대는 경쟁: 같은 값을 동시에 넣어도 유니크 제약과 `ON CONFLICT`가 하나만 남기는지 (같은 새 SKU의 동시 첫 입고)
- 트랜잭션 경계: 중간에 예외가 나면 그 전에 한 쓰기까지 함께 되돌려지는지, 여러 저장이 한 트랜잭션으로 묶이는지
- 격리 수준에 기대는 동작: 다른 트랜잭션이 커밋한 행이 보여야 맞게 도는 코드 (READ COMMITTED에서 다시 찾기)
- 이 중 어디에도 해당하지 않으면 Mockito 서비스 테스트로 쓴다

### 작성 규칙

- 테스트 클래스는 `XxxServiceDbTest`로 두고, `@DataJpaTest`, `@DbContextTest`, `@Import`, `@Transactional(propagation = Propagation.NOT_SUPPORTED)`를 함께 붙인다
- `@Import`에는 테스트할 서비스와 그 서비스가 쓰는 컴포넌트만 적는다. 무엇에 의존하는지가 테스트 머리에 드러난다
- `@DataJpaTest`는 테스트마다 트랜잭션을 걸고 롤백한다. `NOT_SUPPORTED`로 그 트랜잭션을 꺼야 서비스가 실제로 커밋하고, 다른 스레드가 그 결과를 본다
- 롤백되지 않으므로 SKU는 테스트마다 새로 만들고(UUID), 검증은 그 상품의 데이터로 좁힌다
- 동시성 테스트
    - 작업 스레드를 `CountDownLatch`로 한꺼번에 출발시킨다
    - 결과는 `Future.get()`으로 받는다. 한 건이라도 예외가 나면 테스트가 실패한다
    - 최종 수량과 입출고 기록을 함께 검증한다
- 이름, `@DisplayName`, 검증 규칙은 단위 테스트와 같다

### 예시

`movements()`는 그 상품의 입출고 기록을 리포지토리로 읽는 도우미다. 전체 코드는 `InventoryServiceDbTest`에 있다.

```java
@DataJpaTest
@DbContextTest
@Import({InventoryService.class, ProductFinder.class, ProductRegistrar.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InventoryServiceDbTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("등록된 상품에 입고가 동시에 들어와도 수량이 빠짐없이 늘어난다")
    void concurrentInboundAddsEveryQuantity() throws Exception {
        String sku = "SKU-" + UUID.randomUUID();
        inventoryService.inbound(new InboundItem(sku, "콜라", 100));

        runConcurrently(100, () -> inventoryService.inbound(new InboundItem(sku, "콜라", 1)));

        StockStatus status = inventoryService.getStockBySku(sku);
        assertThat(status.stock().getQuantity()).isEqualTo(200);
        assertThat(movements(status.product().getId()))
                .extracting(ProductStockMovementEntity::getQuantityAfter)
                .containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(100, 200).boxed().toList());
    }

    private <T> List<T> runConcurrently(int requests, Callable<T> task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }
}
```

## 통합 테스트 (`SpringBootTest`)

앱 전체를 띄워 HTTP 요청부터 DB까지 한 번에 검증한다. 지금은 앱이 뜨는지만 확인하는 `ApplicationTests` 하나만 있고, 통합 테스트를 만들 때 이 섹션에 규칙을 더한다.
