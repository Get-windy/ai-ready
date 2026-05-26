package cn.aiedge.erp.purchase.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商评估请求DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "供应商评估请求")
public class SupplierEvaluationRequest {

    @Schema(description = "供应商ID", example = "SUP001", required = true)
    @NotBlank(message = "供应商ID不能为空")
    private String supplierId;

    @Schema(description = "评估编号", example = "EVA202605050001")
    @Size(max = 50, message = "评估编号长度不能超过50个字符")
    private String evaluationNo;

    @Schema(description = "评估类型", example = "PERIODIC", required = true, allowableValues = {
        "INITIAL", "PERIODIC", "SPECIAL", "RE_EVALUATION", "QUALITY_AUDIT", 
        "DELIVERY_REVIEW", "PRICE_EVALUATION", "SERVICE_ASSESSMENT"
    })
    @NotBlank(message = "评估类型不能为空")
    private String evaluationType;

    @Schema(description = "评估周期开始日期", example = "2024-01-01")
    @NotNull(message = "评估周期开始日期不能为空")
    private LocalDate periodStart;

    @Schema(description = "评估周期结束日期", example = "2024-12-31")
    @NotNull(message = "评估周期结束日期不能为空")
    private LocalDate periodEnd;

    @Schema(description = "物料编码")
    @Size(max = 50, message = "物料编码长度不能超过50个字符")
    private String materialCode;

    @Schema(description = "物料名称")
    @Size(max = 200, message = "物料名称长度不能超过200个字符")
    private String materialName;

    @Schema(description = "评估标准版本", example = "V1.2.0")
    @Size(max = 50, message = "评估标准版本长度不能超过50个字符")
    private String evaluationStandardVersion = "V1.0.0";

    @Schema(description = "质量评分（0-100）", example = "92.5")
    private BigDecimal qualityScore;

    @Schema(description = "价格评分（0-100）", example = "88.0")
    private BigDecimal priceScore;

    @Schema(description = "交期评分（0-100）", example = "95.0")
    private BigDecimal deliveryScore;

    @Schema(description = "服务评分（0-100）", example = "90.5")
    private BigDecimal serviceScore;

    @Schema(description = "技术评分（0-100）", example = "85.0")
    private BigDecimal technicalScore;

    @Schema(description = "综合评分（0-100）", example = "90.0")
    private BigDecimal overallScore;

    @Schema(description = "信用评分（0-100）", example = "93.0")
    private BigDecimal creditScore;

    @Schema(description = "风险评分（0-100，越高风险越大）", example = "15.5")
    private BigDecimal riskScore;

    @Schema(description = "推荐等级", example = "A", allowableValues = {"A", "B", "C", "D"})
    private String recommendationLevel;

    @Schema(description = "总体评价", example = "优质供应商，产品质量稳定，交期准时")
    @Size(max = 1000, message = "总体评价长度不能超过1000个字符")
    private String overallAssessment;

    @Schema(description = "质量评价", example = "产品质量符合标准，不良率低于1%")
    @Size(max = 1000, message = "质量评价长度不能超过1000个字符")
    private String qualityAssessment;

    @Schema(description = "价格评价", example = "价格合理，具有市场竞争力")
    @Size(max = 1000, message = "价格评价长度不能超过1000个字符")
    private String priceAssessment;

    @Schema(description = "交期评价", example = "交期准时率98%，偶尔有延迟但能及时沟通")
    @Size(max = 1000, message = "交期评价长度不能超过1000个字符")
    private String deliveryAssessment;

    @Schema(description = "服务评价", example = "售后服务响应及时，技术支持专业")
    @Size(max = 1000, message = "服务评价长度不能超过1000个字符")
    private String serviceAssessment;

    @Schema(description = "技术评价", example = "技术实力强，能提供定制化解决方案")
    @Size(max = 1000, message = "技术评价长度不能超过1000个字符")
    private String technicalAssessment;

    @Schema(description = "信用评价", example = "付款信用良好，无逾期记录")
    @Size(max = 1000, message = "信用评价长度不能超过1000个字符")
    private String creditAssessment;

    @Schema(description = "风险评价", example = "经营稳定，财务风险低")
    @Size(max = 1000, message = "风险评价长度不能超过1000个字符")
    private String riskAssessment;

    @Schema(description = "优势", example = "产品质量稳定、交期准时、价格有竞争力")
    @Size(max = 1000, message = "优势长度不能超过1000个字符")
    private String strengths;

    @Schema(description = "劣势", example = "创新能力有待提升、物流成本较高")
    @Size(max = 1000, message = "劣势长度不能超过1000个字符")
    private String weaknesses;

    @Schema(description = "改进建议", example = "建议加强技术创新投入、优化物流方案")
    @Size(max = 1000, message = "改进建议长度不能超过1000个字符")
    private String improvementSuggestions;

    @Schema(description = "合作建议", example = "继续保持合作，考虑扩大合作范围")
    @Size(max = 1000, message = "合作建议长度不能超过1000个字符")
    private String cooperationSuggestions;

    @Schema(description = "质量关键指标", example = "{\"defectRate\": \"0.8%\", \"returnRate\": \"0.2%\"}")
    @Size(max = 2000, message = "质量关键指标长度不能超过2000个字符")
    private String qualityMetrics;

    @Schema(description = "交付关键指标", example = "{\"onTimeRate\": \"98.5%\", \"averageDelay\": \"1.2\"}")
    @Size(max = 2000, message = "交付关键指标长度不能超过2000个字符")
    private String deliveryMetrics;

    @Schema(description = "价格关键指标", example = "{\"priceTrend\": \"stable\", \"marketRank\": \"top20%\"}")
    @Size(max = 2000, message = "价格关键指标长度不能超过2000个字符")
    private String priceMetrics;

    @Schema(description = "服务关键指标", example = "{\"responseTime\": \"<4h\", \"satisfactionRate\": \"95%\"}")
    @Size(max = 2000, message = "服务关键指标长度不能超过2000个字符")
    private String serviceMetrics;

    @Schema(description = "技术关键指标", example = "{\"rdInvestment\": \"8%\", \"patentCount\": \"25\"}")
    @Size(max = 2000, message = "技术关键指标长度不能超过2000个字符")
    private String technicalMetrics;

    @Schema(description = "信用关键指标", example = "{\"paymentDelay\": \"0\", \"creditUtilization\": \"65%\"}")
    @Size(max = 2000, message = "信用关键指标长度不能超过2000个字符")
    private String creditMetrics;

    @Schema(description = "风险关键指标", example = "{\"financialRisk\": \"low\", \"operationalRisk\": \"medium\"}")
    @Size(max = 2000, message = "风险关键指标长度不能超过2000个字符")
    private String riskMetrics;

    @Schema(description = "评估结论", example = "通过", allowableValues = {"PASS", "FAIL", "CONDITIONAL"})
    private String evaluationConclusion;

    @Schema(description = "评估有效期至", example = "2025-12-31")
    private LocalDate evaluationValidUntil;

    @Schema(description = "评估人ID", example = "user001")
    private String evaluatorId;

    @Schema(description = "评估人姓名", example = "张三")
    @Size(max = 100, message = "评估人姓名长度不能超过100个字符")
    private String evaluatorName;

    @Schema(description = "评估部门", example = "采购部")
    @Size(max = 100, message = "评估部门长度不能超过100个字符")
    private String evaluationDepartment;

    @Schema(description = "评估时间", example = "2024-12-15T10:00:00")
    private LocalDateTime evaluationTime;

    @Schema(description = "附件列表")
    private List<AttachmentInfo> attachments;

    @Schema(description = "备注", example = "本次评估基于实际采购数据和现场审核")
    @Size(max = 2000, message = "备注长度不能超过2000个字符")
    private String remarks;

    @Schema(description = "是否自动生成评估报告", example = "true")
    private Boolean autoGenerateReport = true;

    @Schema(description = "评估报告模板", example = "STANDARD")
    private String reportTemplate = "STANDARD";

    /**
     * 附件信息
     */
    @Data
    @Schema(description = "附件信息")
    public static class AttachmentInfo {
        
        @Schema(description = "附件名称", example = "质量检测报告.pdf")
        private String fileName;
        
        @Schema(description = "附件类型", example = "PDF")
        private String fileType;
        
        @Schema(description = "附件大小（字节）", example = "1024000")
        private Long fileSize;
        
        @Schema(description = "附件URL", example = "/attachments/quality-report-2024.pdf")
        private String fileUrl;
        
        @Schema(description = "附件说明", example = "第三方检测机构出具的质量检测报告")
        private String description;
    }
}