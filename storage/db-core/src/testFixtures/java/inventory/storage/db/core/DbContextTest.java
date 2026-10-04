package inventory.storage.db.core;

import org.junit.jupiter.api.Tag;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 스프링 컨텍스트와 PostgreSQL 컨테이너가 필요한 테스트에 붙인다. Docker가 있어야 돈다.
 * test 프로필을 켜서 {@code db-core.yaml}의 test 설정을 쓴다.
 * {@code ./gradlew dbContextTest}로 이 테스트만 돌리고, {@code ./gradlew unitTest}에서는 빠진다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("db-context")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public @interface DbContextTest {
}
