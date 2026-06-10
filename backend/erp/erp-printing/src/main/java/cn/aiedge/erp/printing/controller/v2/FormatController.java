package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.ValidateFormulaRequest;
import cn.aiedge.erp.printing.dto.v2.ValidateFormulaResponse;
import cn.aiedge.erp.printing.engine.FormatEngine;
import cn.aiedge.erp.printing.engine.FormulaValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "V2-格式化引擎", description = "自定义函数校验、字段格式化、模板 HTML 渲染")
@RestController
@RequestMapping("/api/v2/print/format")
@RequiredArgsConstructor
public class FormatController {

    private final FormatEngine formatEngine;

    @Operation(summary = "校验自定义函数表达式合法性")
    @PostMapping("/validate-formula")
    public ResponseEntity<ApiResponse<Object>> validateFormula(
            @Valid @RequestBody ValidateFormulaRequest request) {
        FormulaValidationResult result = formatEngine.validateExpression(
                request.getExpression(), request.getSampleValue());

        ValidateFormulaResponse response = new ValidateFormulaResponse();
        response.setValid(result.isValid());
        response.setPreviewResult(result.getPreviewResult());
        response.setErrorMessage(result.getErrorMessage());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "渲染模板为 HTML")
    @PostMapping("/render")
    public ResponseEntity<ApiResponse<Object>> render(
            @RequestBody Map<String, Object> request) {
        String templateJson = (String) request.getOrDefault("templateJson", "{}");
        String dataJson = (String) request.getOrDefault("dataJson", "{}");
        String html = formatEngine.renderToHtml(templateJson, dataJson);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("html", html)));
    }

    @Operation(summary = "格式化单个字段值")
    @PostMapping("/field")
    public ResponseEntity<ApiResponse<Object>> formatField(@RequestBody Map<String, Object> request) {
        Object value = request.get("value");
        @SuppressWarnings("unchecked")
        Map<String, Object> formatConfig = (Map<String, Object>) request.get("formatConfig");
        String result = formatEngine.format(value, formatConfig);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("result", result)));
    }
}
