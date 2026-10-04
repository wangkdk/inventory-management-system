package inventory;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 스프링 MVC 슬라이스(WebMvcTest)로 웹 계층만 띄우는 테스트에 붙인다. DB와 Docker 없이 돈다.
 * REST Docs가 켜져 있어서 테스트에서 document()로 API 문서 조각을 만들 수 있다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("web-context")
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
public @interface WebContextTest {
}
