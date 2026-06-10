package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateFormulaRequest {

    @NotBlank(message = "表达式不能为空")
    private String expression;

    private Object sampleValue;
}
