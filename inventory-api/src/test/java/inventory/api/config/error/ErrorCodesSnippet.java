package inventory.api.config.error;

import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ErrorCode의 모든 코드를 API 문서의 표로 만든다. 표 모양은 error-codes.snippet 템플릿이 정한다.
 */
public class ErrorCodesSnippet extends TemplatedSnippet {

    public ErrorCodesSnippet() {
        super("error-codes", null);
    }

    @Override
    protected Map<String, Object> createModel(Operation operation) {
        List<Map<String, Object>> codes = Arrays.stream(ErrorCode.values())
                .map(code -> Map.<String, Object>of(
                        "code", code.name(),
                        "status", code.getStatus().value(),
                        "title", code.getTitle()))
                .toList();
        // REST Docs가 이 맵에 스니펫 속성을 더 넣으므로 바꿀 수 있는 맵으로 돌려준다
        Map<String, Object> model = new HashMap<>();
        model.put("codes", codes);
        return model;
    }
}
