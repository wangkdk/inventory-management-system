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
- 스프링 컨텍스트, mock, DB를 쓰지 않는다.

### 예시

```java
@Test
@DisplayName("상품을 등록하면 SKU의 앞뒤 공백을 제거한다")
void registerStripsSku() {
    Product product = Product.register("  SKU-001 ", "콜라");

    assertThat(product.getSku()).isEqualTo("SKU-001");
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
- `@DataJpaTest`는 엔티티와 Spring Data 리포지토리만 띄운다. `@Service`나 직접 만든 `@Repository` 클래스는 빈으로 올라오지 않는다. 서비스까지 검증하려면 `@SpringBootTest`를 쓴다
- 테스트마다 트랜잭션이 걸리고 끝나면 롤백된다. 테스트끼리 데이터를 지우지 않아도 된다
- 한 트랜잭션 안에서 도는 테스트라서 동시성은 여기서 검증하지 않는다. 동시성 테스트는 요청마다 트랜잭션이 따로 열리는 `@SpringBootTest`로 쓴다
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
