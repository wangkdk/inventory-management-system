package inventory;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 스프링 MVC 슬라이스(WebMvcTest)로 웹 계층만 띄우는 테스트에 붙인다. DB와 Docker 없이 돈다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("web-context")
public @interface WebContextTest {
}
