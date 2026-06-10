package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

@Data
public class ValidateFormulaResponse {

    private boolean valid;
    private Object previewResult;
    private String errorMessage;
}
