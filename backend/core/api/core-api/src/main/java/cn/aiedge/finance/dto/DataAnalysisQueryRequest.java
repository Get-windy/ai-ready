package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/**
 * 数据分析查询请求
 */
@Data
@Schema(description = "数据分析查询请求")
public class DataAnalysisQueryRequest {

    @Schema(description = "分析类型")
    private String analysisType; // FINANCIAL-财务分析, OPERATIONAL-运营分析, SALES-销售分析, CUSTOMER-客户分析

    @Schema(description = "分析分类")
    private String analysisCategory; // RATIO-比率分析, TREND-趋势分析, COMPARATIVE-对比分析, PREDICTIVE-预测分析

    @Schema(description = "分析名称")
    private String analysisName;

    @Schema(description = "状态")
    private String status; // DRAFT-草稿, PROCESSING-处理中, COMPLETED-已完成, FAILED-失败

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "分析员")
    private String analyzer;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
