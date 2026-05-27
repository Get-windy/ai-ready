package cn.aiedge.erp.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商绩效评估数据传输对象
 */
@Data
@Schema(description = "供应商绩效评估数据传输对象")
public class SupplierPerformanceDTO {
    
    @Schema(description = "绩效评估ID", example = "1")
    private Long id;
    
    @NotNull(message = "供应商ID不能为空")
    @Schema(description = "供应商ID", example = "1", required = true)
    private Long supplierId;
    
    @Schema(description = "供应商编码", example = "SUP20240001")
    private String supplierCode;
    
    @Schema(description = "供应商名称", example = "北京科技有限公司")
    private String supplierName;
    
    @NotNull(message = "评估周期不能为空")
    @Schema(description = "评估周期：yyyy-MM（月度评估），yyyy-Q（季度评估），yyyy（年度评估）", example = "2024-01", required = true)
    private String evaluationPeriod;
    
    @NotNull(message = "评估类型不能为空")
    @Schema(description = "评估类型：1-月度评估，2-季度评估，3-年度评估，4-专项评估", example = "1", required = true)
    private Integer evaluationType;
    
    @Schema(description = "评估日期", example = "2024-01-15T00:00:00")
    private LocalDateTime evaluationDate;
    
    @Schema(description = "评估人ID", example = "user001")
    private String evaluatorId;
    
    @Schema(description = "评估人姓名", example = "张三")
    private String evaluatorName;
    
    @Schema(description = "质量评分（0-100）", example = "95.0")
    private Double qualityScore;
    
    @Schema(description = "交付评分（0-100）", example = "90.0")
    private Double deliveryScore;
    
    @Schema(description = "价格评分（0-100）", example = "85.0")
    private Double priceScore;
    
    @Schema(description = "服务评分（0-100）", example = "92.0")
    private Double serviceScore;
    
    @Schema(description = "技术评分（0-100）", example = "88.0")
    private Double technologyScore;
    
    @Schema(description = "响应速度评分（0-100）", example = "88.0")
    private Double responseScore;
    
    @Schema(description = "合规性评分（0-100）", example = "95.0")
    private Double complianceScore;
    
    @Schema(description = "综合评分（0-100）", example = "90.5")
    private Double comprehensiveScore;
    
    @Schema(description = "综合评分（BigDecimal）", example = "93.75")
    private BigDecimal overallScore;
    
    @Schema(description = "等级评定：A/B/C/D", example = "A")
    private String performanceLevel;
    
    @Schema(description = "等级变化：1-升级，0-不变，-1-降级", example = "0")
    private Integer levelChange;
    
    @Schema(description = "质量扣分项")
    private String qualityDeductions;
    
    @Schema(description = "交付扣分项")
    private String deliveryDeductions;
    
    @Schema(description = "服务扣分项")
    private String serviceDeductions;
    
    @Schema(description = "主要优势")
    private String strengths;
    
    @Schema(description = "改进建议")
    private String improvementSuggestions;
    
    @Schema(description = "评估结论")
    private String evaluationConclusion;
    
    @Schema(description = "附件信息")
    private String attachmentInfo;
    
    @Schema(description = "创建人", example = "admin")
    private String createBy;
    
    @Schema(description = "创建时间", example = "2024-01-01T00:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    
    @Schema(description = "更新时间", example = "2024-01-01T00:00:00")
    private LocalDateTime updateTime;
}