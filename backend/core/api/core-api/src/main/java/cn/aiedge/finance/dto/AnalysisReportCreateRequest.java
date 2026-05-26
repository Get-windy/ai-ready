package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * 分析报告创建请求
 */
@Data
@Schema(description = "分析报告创建请求")
public class AnalysisReportCreateRequest {

    @Schema(description = "报告名称", required = true)
    @NotBlank(message = "报告名称不能为空")
    private String reportName;

    @Schema(description = "报告类型", required = true)
    @NotBlank(message = "报告类型不能为空")
    private String reportType; // DASHBOARD-仪表板, SUMMARY-汇总报告, DETAILED-详细报告, COMPARISON-对比报告

    @Schema(description = "报告分类", required = true)
    @NotBlank(message = "报告分类不能为空")
    private String reportCategory; // FINANCIAL-财务报告, OPERATIONAL-运营报告, SALES-销售报告, CUSTOMER-客户报告

    @Schema(description = "报告日期", required = true)
    @NotNull(message = "报告日期不能为空")
    private LocalDate reportDate;

    @Schema(description = "期间")
    private String period;

    @Schema(description = "报告期间类型")
    private String reportPeriodType; // DAILY-日, WEEKLY-周, MONTHLY-月, QUARTERLY-季度, YEARLY-年

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "包含的分析ID列表")
    private String analysisIds; // 逗号分隔

    @Schema(description = "报告内容")
    private String reportContent; // HTML/Markdown格式

    @Schema(description = "执行摘要")
    private String executiveSummary;

    @Schema(description = "结论")
    private String conclusions;

    @Schema(description = "建议")
    private String recommendations;

    @Schema(description = "可见性")
    private String visibility; // PUBLIC-公开, PRIVATE-私有, TEAM-团队, DEPARTMENT-部门

    @Schema(description = "报告格式")
    private String reportFormat; // PDF, EXCEL, WORD, HTML, POWERPOINT

    @Schema(description = "报告模板")
    private String reportTemplate;

    @Schema(description = "图表配置")
    private String chartConfiguration; // JSON格式

    @Schema(description = "过滤器配置")
    private String filterConfiguration; // JSON格式

    @Schema(description = "标签")
    private String tags; // 逗号分隔

    @Schema(description = "关键词")
    private String keywords; // 逗号分隔
}
