package cn.aiedge.erp.supplierbatchanalysis.dto;

import cn.aiedge.erp.supplierbatchanalysis.enums.AnalysisTypeEnum;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商批次关联分析请求DTO
 *
 * @author team-member
 * @date 2026-04-30
 */
@Data
public class SupplierBatchAnalysisRequest {
    
    /**
     * 供应商ID列表（为空时分析所有供应商）
     */
    private List<Long> supplierIds;
    
    /**
     * 产品ID列表（为空时分析所有产品）
     */
    private List<Long> productIds;
    
    /**
     * 分析开始时间
     */
    @NotNull(message = "分析开始时间不能为空")
    private LocalDateTime startDate;
    
    /**
     * 分析结束时间
     */
    @NotNull(message = "分析结束时间不能为空")
    private LocalDateTime endDate;
    
    /**
     * 分析类型
     */
    private AnalysisTypeEnum analysisType = AnalysisTypeEnum.CUSTOM;
    
    /**
     * 是否包含趋势分析
     */
    private Boolean includeTrendAnalysis = false;
    
    /**
     * 是否包含预测分析
     */
    private Boolean includePredictionAnalysis = false;
    
    /**
     * 是否包含对比分析
     */
    private Boolean includeComparativeAnalysis = false;
    
    /**
     * 批次质量阈值（用于分类）
     */
    private Double qualityThreshold = 70.0;
    
    /**
     * 供应商绩效阈值（用于分类）
     */
    private Double supplierScoreThreshold = 70.0;
    
    /**
     * 最小批次数量要求
     */
    private Integer minBatchCount = 10;
    
    /**
     * 关联度阈值（用于显著性判断）
     */
    private Double correlationThreshold = 0.5;
    
    /**
     * 是否生成可视化报告
     */
    private Boolean generateReport = true;
    
    /**
     * 是否生成预警建议
     */
    private Boolean generateRecommendations = true;
    
    /**
     * 自定义参数（JSON格式）
     */
    private String customParameters;
    
    /**
     * 分析备注
     */
    private String remark;
}