package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * 数据分析创建请求
 */
@Data
@Schema(description = "数据分析创建请求")
public class DataAnalysisCreateRequest {

    @Schema(description = "分析名称", required = true)
    @NotBlank(message = "分析名称不能为空")
    private String analysisName;

    @Schema(description = "分析类型", required = true)
    @NotBlank(message = "分析类型不能为空")
    private String analysisType; // FINANCIAL-财务分析, OPERATIONAL-运营分析, SALES-销售分析, CUSTOMER-客户分析

    @Schema(description = "分析分类", required = true)
    @NotBlank(message = "分析分类不能为空")
    private String analysisCategory; // RATIO-比率分析, TREND-趋势分析, COMPARATIVE-对比分析, PREDICTIVE-预测分析

    @Schema(description = "开始日期", required = true)
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", required = true)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "数据来源", required = true)
    @NotBlank(message = "数据来源不能为空")
    private String dataSource;

    @Schema(description = "分析方法")
    private String analysisMethod;

    @Schema(description = "分析公式")
    private String analysisFormula;

    @Schema(description = "分析参数")
    private String parameters; // JSON格式

    @Schema(description = "分析备注")
    private String analysisNotes;

    @Schema(description = "分析建议")
    private String recommendation;
}
