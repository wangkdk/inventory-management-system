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

모든 테스트를 돌리려면 Docker가 켜져 있어야 합니다. Docker가 없으면 DB를 쓰는 테스트는 건너뛰지 않고 실패합니다.

```bash
./gradlew test            # 전체
./gradlew unitTest        # Docker 없이 도는 단위 테스트만
./gradlew webContextTest  # 웹 계층만 띄우는 컨트롤러 테스트만 (Docker 불필요)
./gradlew dbContextTest   # PostgreSQL 컨테이너가 필요한 테스트만
```

테스트 작성 규칙은 [테스트 가이드](docs/test-guide.md)에 있습니다.

### 애플리케이션 실행

로컬 DB를 띄운 뒤 애플리케이션을 실행합니다. 기본 프로필은 `local`입니다.

```bash
docker compose up -d
./gradlew bootRun
```

로컬 DB를 내릴 때

```bash
docker compose down      # 컨테이너만 지우고 데이터는 볼륨에 남습니다
docker compose down -v   # 데이터까지 지웁니다
```