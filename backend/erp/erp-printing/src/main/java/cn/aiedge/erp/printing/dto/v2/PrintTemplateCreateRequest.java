package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PrintTemplateCreateRequest {

    @NotBlank(message = "pageCode 不能为空")
    private String pageCode;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 200)
    private String templateName;

    private String templateJson;

    private String paperSize;

    private BigDecimal paperWidth;

    private BigDecimal paperHeight;

    private BigDecimal marginTop;

    private BigDecimal marginBottom;

    private BigDecimal marginLeft;

    private BigDecimal marginRight;

    private Boolean isDefault;
}
