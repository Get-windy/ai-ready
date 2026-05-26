package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报表模板创建请求
 */
@Data
@Schema(description = "报表模板创建请求")
public class ReportTemplateCreateRequest {

    @Schema(description = "模板名称", required = true)
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "模板类型", required = true)
    @NotBlank(message = "模板类型不能为空")
    private String templateType; // FINANCIAL-财务报表, SALES-销售报表, INVENTORY-库存报表, CUSTOM-自定义

    @Schema(description = "模板分类")
    private String category;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "模板配置")
    private String templateConfig; // JSON格式

    @Schema(description = "字段映射")
    private String fieldMappings; // JSON格式

    @Schema(description = "数据过滤器")
    private String dataFilters; // JSON格式

    @Schema(description = "权限配置")
    private String permissions; // JSON格式

    @Schema(description = "SQL查询语句")
    private String sqlQuery;

    @Schema(description = "数据源ID")
    private String dataSourceId;

    @Schema(description = "图表配置")
    private String chartConfigs; // JSON格式

    @Schema(description = "是否默认模板")
    private Boolean isDefault;

    @Schema(description = "是否激活")
    private Boolean isActive;

    @Schema(description = "排序")
    private Integer sortOrder;
}
