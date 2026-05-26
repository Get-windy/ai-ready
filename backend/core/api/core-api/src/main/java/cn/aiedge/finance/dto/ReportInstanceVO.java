package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报表实例VO
 */
@Data
@Schema(description = "报表实例详情")
public class ReportInstanceVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "报表编码")
    private String reportCode;

    @Schema(description = "报表名称")
    private String reportName;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "报表类型")
    private String reportType;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "期间")
    private String period;

    @Schema(description = "报告期间类型")
    private String reportPeriodType; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY

    @Schema(description = "状态")
    private String status; // DRAFT, GENERATING, COMPLETED, FAILED, PUBLISHED, ARCHIVED

    @Schema(description = "执行日志")
    private String executionLog;

    @Schema(description = "执行耗时")
    private Long executionTime; // 毫秒

    @Schema(description = "报表数据")
    private String reportData; // JSON格式

    @Schema(description = "图表数据")
    private String chartData; // JSON格式

    @Schema(description = "导出文件路径")
    private String exportPath;

    @Schema(description = "应用的过滤器")
    private String filtersApplied; // JSON格式

    @Schema(description = "参数")
    private String parameters; // JSON格式

    @Schema(description = "生成人")
    private String generatedBy;

    @Schema(description = "发布人")
    private String publishedBy;

    @Schema(description = "导出人")
    private String exportedBy;

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

    @Schema(description = "查看次数")
    private Integer viewCount;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
