package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/**
 * 分析报告查询请求
 */
@Data
@Schema(description = "分析报告查询请求")
public class AnalysisReportQueryRequest {

    @Schema(description = "报告类型")
    private String reportType; // DASHBOARD-仪表板, SUMMARY-汇总报告, DETAILED-详细报告, COMPARISON-对比报告

    @Schema(description = "报告分类")
    private String reportCategory; // FINANCIAL-财务报告, OPERATIONAL-运营报告, SALES-销售报告, CUSTOMER-客户报告

    @Schema(description = "报告名称")
    private String reportName;

    @Schema(description = "状态")
    private String status; // DRAFT-草稿, REVIEWING-审核中, APPROVED-已批准, PUBLISHED-已发布, ARCHIVED-已归档

    @Schema(description = "报告日期")
    private LocalDate reportDate;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
