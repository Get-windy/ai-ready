package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PrintTemplateVO {

    private Long templateId;
    private Long tenantId;
    private String pageCode;
    private String templateName;

    /**
     * 解析后的模板 Map 结构
     */
    private Map<String, Object> templateJson;

    private String paperSize;
    private BigDecimal paperWidth;
    private BigDecimal paperHeight;
    private BigDecimal marginTop;
    private BigDecimal marginBottom;
    private BigDecimal marginLeft;
    private BigDecimal marginRight;
    private String status;
    private Boolean isDefault;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
