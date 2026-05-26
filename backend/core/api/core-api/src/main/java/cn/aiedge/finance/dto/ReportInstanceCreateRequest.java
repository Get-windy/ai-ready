package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

/**
 * 报表实例创建请求
 */
@Data
@Schema(description = "报表实例创建请求")
public class ReportInstanceCreateRequest {

    @Schema(description = "报表名称", required = true)
    @NotBlank(message = "报表名称不能为空")
    private String reportName;

    @Schema(description = "模板ID", required = true)
    private Long templateId;

    @Schema(description = "报表类型", required = true)
    @NotBlank(message = "报表类型不能为空")
    private String reportType;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "期间")
    private String period;

    @Schema(description = "报告期间类型")
    private String reportPeriodType; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY

    @Schema(description = "应用的过滤器")
    private String filtersApplied; // JSON格式

    @Schema(description = "参数")
    private String parameters; // JSON格式

    @Schema(description = "权限配置")
    private String permissions; // JSON格式

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否收藏")
    private Boolean isFavorite;

    @Schema(description = "是否共享")
    private Boolean isShared;
}
