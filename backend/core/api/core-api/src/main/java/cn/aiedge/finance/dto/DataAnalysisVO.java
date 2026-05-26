package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据分析VO
 */
@Data
@Schema(description = "数据分析详情")
public class DataAnalysisVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分析编码")
    private String analysisCode;

    @Schema(description = "分析名称")
    private String analysisName;

    @Schema(description = "分析类型")
    private String analysisType; // FINANCIAL-财务分析, OPERATIONAL-运营分析, SALES-销售分析, CUSTOMER-客户分析

    @Schema(description = "分析分类")
    private String analysisCategory; // RATIO-比率分析, TREND-趋势分析, COMPARATIVE-对比分析, PREDICTIVE-预测分析

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "期间")
    private String period;

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "分析方法")
    private String analysisMethod;

    @Schema(description = "分析公式")
    private String analysisFormula;

    @Schema(description = "分析参数")
    private String parameters; // JSON格式

    @Schema(description = "状态")
    private String status; // DRAFT-草稿, PROCESSING-处理中, COMPLETED-已完成, FAILED-失败

    @Schema(description = "执行结果")
    private String executionResult; // JSON格式

    @Schema(description = "可视化数据")
    private String visualizationData; // JSON格式

    @Schema(description = "分析员")
    private String analyzer;

    @Schema(description = "分析备注")
    private String analysisNotes;

    @Schema(description = "置信度")
    private BigDecimal confidenceLevel;

    @Schema(description = "分析建议")
    private String recommendation;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
