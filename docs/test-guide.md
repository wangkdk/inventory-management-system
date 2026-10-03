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
