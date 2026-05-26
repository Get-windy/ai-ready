package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分析报告VO
 */
@Data
@Schema(description = "分析报告详情")
public class AnalysisReportVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "报告编码")
    private String reportCode;

    @Schema(description = "报告名称")
    private String reportName;

    @Schema(description = "报告类型")
    private String reportType; // DASHBOARD-仪表板, SUMMARY-汇总报告, DETAILED-详细报告, COMPARISON-对比报告

    @Schema(description = "报告分类")
    private String reportCategory; // FINANCIAL-财务报告, OPERATIONAL-运营报告, SALES-销售报告, CUSTOMER-客户报告

    @Schema(description = "报告日期")
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

    @Schema(description = "状态")
    private String status; // DRAFT-草稿, REVIEWING-审核中, APPROVED-已批准, PUBLISHED-已发布, ARCHIVED-已归档

    @Schema(description = "可见性")
    private String visibility; // PUBLIC-公开, PRIVATE-私有, TEAM-团队, DEPARTMENT-部门

    @Schema(description = "作者")
    private String author;

    @Schema(description = "审核人")
    private String reviewer;

    @Schema(description = "批准人")
    private String approver;

    @Schema(description = "发布人")
    private String publisher;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
