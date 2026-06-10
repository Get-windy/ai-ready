package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ScreenshotCreateRequest {

    @NotNull(message = "templateId 不能为空")
    private Long templateId;

    @NotNull(message = "dataJson 不能为空")
    private Map<String, Object> dataJson;

    private String pageCode;
}
