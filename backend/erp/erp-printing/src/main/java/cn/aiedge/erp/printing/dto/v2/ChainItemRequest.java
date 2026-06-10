package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChainItemRequest {

    @Min(1) @Max(10)
    @NotNull
    private Integer stepOrder;

    @NotNull
    private Long templateId;

    @NotNull
    private Long clientId;

    private String printerName;

    /**
     * 截图模式: DISABLED / MANUAL_CONFIRM / AUTO_CONFIRM，默认 DISABLED
     */
    private String screenshotMode;

    private Integer screenshotConfirmTimeout;

    /**
     * 截图自定义配置：可自定义截图内容、布局、水印等
     */
    private String screenshotConfigJson;
}
