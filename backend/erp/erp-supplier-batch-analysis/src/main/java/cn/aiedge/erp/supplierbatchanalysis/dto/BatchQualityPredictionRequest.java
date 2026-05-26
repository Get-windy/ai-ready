package cn.aiedge.erp.supplierbatchanalysis.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 批次质量预测请求DTO
 *
 * @author team-member
 * @date 2026-04-30
 */
@Data
public class BatchQualityPredictionRequest {
    
    /**
     * 批次号
     */
    @NotBlank(message = "批次号不能为空")
    private String batchNo;
    
    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    private Long productId;
    
    /**
     * 产品编码
     */
    private String productCode;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 供应商ID
     */
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;
    
    /**
     * 供应商编码
     */
    private String supplierCode;
    
    /**
     * 供应商名称
     */
    private String supplierName;
    
    /**
     * 批次数量
     */
    @NotNull(message = "批次数量不能为空")
    private BigDecimal batchQuantity;
    
    /**
     * 生产日期（字符串格式）
     */
    @NotBlank(message = "生产日期不能为空")
    private String productionDate;
    
    /**
     * 供应商绩效得分（可选，如为空则从系统获取）
     */
    private BigDecimal supplierPerformanceScore;
    
    /**
     * 供应商绩效等级（可选）
     */
    private String supplierPerformanceLevel;
    
    /**
     * 产品历史平均合格率
     */
    private BigDecimal productHistoricalQualificationRate;
    
    /**
     * 供应商历史平均合格率
     */
    private BigDecimal supplierHistoricalQualificationRate;
    
    /**
     * 模型ID（为空使用默认模型）
     */
    private String modelId;
    
    /**
     * 模型参数（JSON格式）
     */
    private String modelParameters;
    
    /**
     * 批次特征（JSON格式）
     */
    private Map<String, Object> batchFeatures;
    
    /**
     * 是否生成详细预测报告
     */
    private Boolean generateDetailReport = false;
    
    /**
     * 是否生成风险预警
     */
    private Boolean generateRiskWarning = true;
    
    /**
     * 是否生成改进建议
     */
    private Boolean generateRecommendations = true;
    
    /**
     * 预测备注
     */
    private String remark;
}