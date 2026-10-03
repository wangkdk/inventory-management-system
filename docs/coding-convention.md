# 코딩 컨벤션

## 의존 방향

순환 참조가 생기지 않도록 의존은 한 방향으로만 흐르게 한다.

### 패키지

- `api → domain → storage` 한 방향으로만 참조한다
- storage는 domain을 참조하지 않는다. 엔티티와 도메인 객체의 변환은 domain에서 한다
- domain 안에서는 재고(`stock`)가 상품(`product`)을 참조하고, 상품은 재고를 참조하지 않는다
- 두 도메인에 걸친 유스케이스는 다른 쪽을 참조할 수 있는 도메인에 둔다. 상품과 재고 수량을 함께 조회하는 일은 재고 쪽 `InventoryService`가 맡는다

### 서비스와 컴포넌트

- `@Service`는 유스케이스의 입구다. 컨트롤러가 부르고, 트랜잭션은 서비스 메서드가 묶는다
- 서비스는 다른 서비스를 부르지 않는다. 여러 서비스가 함께 쓰는 일은 `@Component`로 만들고, 서비스는 컴포넌트를 부른다
- 컴포넌트끼리는 불러도 된다
- 컴포넌트에는 꼭 필요할 때만 `@Transactional`을 둔다(예: `@Modifying` 쿼리를 쓰는 `ProductRegistrar`). 서비스 트랜잭션 안에서 부르면 거기에 합류한다

```text
Controller → Service → Component → Repository
```

예를 들어 `InventoryService`는 상품을 찾을 때 `ProductService`가 아니라 `ProductFinder`를 부른다. 나중에 상품만 다루는 기능이 생겨 `ProductService`를 만들어도, 그 서비스 역시 `ProductFinder`를 부른다. 두 서비스는 서로를 부르지 않는다.
