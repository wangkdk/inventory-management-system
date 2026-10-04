# inventory-management-system

재고 관리 시스템의 MVP입니다.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Gradle | 9.7.1 |
| PostgreSQL | 18 |

## 실행 방법

### 사전 준비

- JDK 25
- Docker (테스트와 로컬 DB에 필요)

### 테스트 실행

test, dbContextTest를 돌리려면 Docker가 켜져 있어야 합니다. Docker가 없으면 DB를 쓰는 테스트는 건너뛰지 않고 실패합니다.

```bash
./gradlew test            # 전체
./gradlew unitTest        # Docker 없이 도는 단위 테스트만
./gradlew webContextTest  # Docker 없이 웹 계층만 띄우는 컨트롤러 테스트만
./gradlew dbContextTest   # PostgreSQL 컨테이너가 필요한 테스트만
```

테스트 작성 규칙 [테스트 가이드](docs/test-guide.md)

### 애플리케이션 실행

로컬 DB를 띄운 뒤 애플리케이션을 실행합니다. 기본 프로필은 `local`이고, 앱이 뜰 때 [schema.sql](storage/db-core/src/main/resources/schema.sql)로 테이블을 만듭니다.

```bash
docker compose up -d
./gradlew :inventory-api:bootRun
```

앱은 `http://localhost:8080`에서 뜹니다. 요청 예시는 [http/inventory.http](http/inventory.http)에 있습니다.

로컬 DB를 내릴 때

```bash
docker compose down      # 컨테이너만 지우고 데이터는 볼륨에 남습니다
docker compose down -v   # 데이터까지 지웁니다
```

### API 문서

API 문서는 Spring REST Docs로 만듭니다.

```bash
./gradlew :inventory-api:asciidoctor
```

만들어진 문서의 위치는 `inventory-api/build/docs/asciidoc/index.html`입니다.

## 모듈 구조

```text
inventory-management-system
├── inventory-api      스프링 부트 앱. 컨트롤러, 에러 응답, API 문서
├── domain             재고 규칙과 유스케이스, 저장소 인터페이스
└── storage
    └── db-core        저장소 구현(JPA), 엔티티, schema.sql
```

의존 방향은 `inventory-api -> domain <- storage:db-core`입니다. 

모듈과 패키지 규칙 [코딩 컨벤션](docs/coding-convention.md)

## DB 스키마 (DDL)

[storage/db-core/src/main/resources/schema.sql](storage/db-core/src/main/resources/schema.sql)  
