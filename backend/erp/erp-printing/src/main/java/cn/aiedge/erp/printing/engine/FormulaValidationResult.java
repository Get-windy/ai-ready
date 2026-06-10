package cn.aiedge.erp.printing.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormulaValidationResult {
    private boolean valid;
    private Object previewResult;
    private String errorMessage;
}
