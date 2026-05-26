package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报表模板查询请求
 */
@Data
@Schema(description = "报表模板查询请求")
public class ReportTemplateQueryRequest {

    @Schema(description = "模板类型")
    private String templateType; // FINANCIAL-财务报表, SALES-销售报表, INVENTORY-库存报表, CUSTOM-自定义

    @Schema(description = "模板分类")
    private String category;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "是否激活")
    private Boolean isActive;

    @Schema(description = "是否默认模板")
    private Boolean isDefault;

    @Schema(description = "数据源ID")
    private String dataSourceId;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
