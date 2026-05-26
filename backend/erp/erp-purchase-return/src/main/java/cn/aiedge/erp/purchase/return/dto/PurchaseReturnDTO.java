package cn.aiedge.erp.purchase.return.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购换货单数据传输对象
 * 
 * 功能: 用于前端与后端的采购换货数据交互
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PurchaseReturnDTO {
    
    /**
     * 换货单ID (更新时使用)
     */
    private Long id;
    
    /**
     * 换货单号 (自动生成)
     */
    private String returnCode;
    
    /**
     * 原采购订单号
     */
    @NotBlank(message = "原采购订单号不能为空")
    private String purchaseOrderCode;
    
    /**
     * 供应商ID
     */
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;
    
    /**
     * 供应商名称
     */
    private String supplierName;
    
    /**
     * 换货类型
     */
    @NotBlank(message = "换货类型不能为空")
    @Pattern(regexp = "quality|quantity|specification", message = "换货类型必须是quality(质量换货)、quantity(数量补货)或specification(规格换货)")
    private String returnType;
    
    /**
     * 换货原因编码
     */
    @NotBlank(message = "换货原因编码不能为空")
    private String returnReasonCode;
    
    /**
     * 换货原因描述
     */
    @Size(max = 500, message = "换货原因描述不能超过500字符")
    private String returnReason;
    
    /**
     * 期望处理方式
     */
    @NotBlank(message = "期望处理方式不能为空")
    @Pattern(regexp = "return_and_replace|replacement|return_and_refund", 
            message = "期望处理方式必须是return_and_replace(退换货)、replacement(只换不退)或return_and_refund(退货退款)")
    private String expectedSolution;
    
    /**
     * 换货状态 (前端传递时可选)
     */
    private String status;
    
    /**
     * 审批流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 换货申请日期
     */
    @NotNull(message = "申请日期不能为空")
    private LocalDateTime applyDate;
    
    /**
     * 期望完成日期
     */
    @NotNull(message = "期望完成日期不能为空")
    @Future(message = "期望完成日期必须是未来日期")
    private LocalDateTime expectedCompleteDate;
    
    /**
     * 申请人ID
     */
    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
    
    /**
     * 申请部门ID
     */
    private Long departmentId;
    
    /**
     * 申请部门名称
     */
    private String departmentName;
    
    /**
     * 紧急程度
     */
    @NotBlank(message = "紧急程度不能为空")
    @Pattern(regexp = "normal|urgent|critical", message = "紧急程度必须是normal(一般)、urgent(紧急)或critical(特急)")
    private String urgencyLevel;
    
    /**
     * 优先级 (1-5)
     */
    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级最小值为1")
    @Max(value = 5, message = "优先级最大值为5")
    private Integer priority;
    
    /**
     * 换货总金额
     */
    @DecimalMin(value = "0.00", inclusive = false, message = "换货总金额必须大于0")
    private BigDecimal totalAmount;
    
    /**
     * 运费
     */
    @DecimalMin(value = "0.00", message = "运费不能为负数")
    private BigDecimal shippingFee = BigDecimal.ZERO;
    
    /**
     * 处理费用
     */
    @DecimalMin(value = "0.00", message = "处理费用不能为负数")
    private BigDecimal handlingFee = BigDecimal.ZERO;
    
    /**
     * 其他费用
     */
    @DecimalMin(value = "0.00", message = "其他费用不能为负数")
    private BigDecimal otherFee = BigDecimal.ZERO;
    
    /**
     * 费用分摊方式
     */
    @NotBlank(message = "费用分摊方式不能为空")
    @Pattern(regexp = "supplier|buyer|share", message = "费用分摊方式必须是supplier(供应商承担)、buyer(采购方承担)或share(双方分摊)")
    private String feeAllocationMethod;
    
    /**
     * 供应商承担比例 (0-100)
     */
    @Min(value = 0, message = "供应商承担比例不能小于0")
    @Max(value = 100, message = "供应商承担比例不能大于100")
    private Integer supplierShareRatio = 100;
    
    /**
     * 供应商沟通记录摘要
     */
    @Size(max = 1000, message = "供应商沟通记录摘要不能超过1000字符")
    private String supplierCommunicationSummary;
    
    /**
     * 供应商确认状态
     */
    private String supplierConfirmationStatus = "pending";
    
    /**
     * 供应商确认时间
     */
    private LocalDateTime supplierConfirmedAt;
    
    /**
     * 供应商确认备注
     */
    @Size(max = 500, message = "供应商确认备注不能超过500字符")
    private String supplierConfirmationNote;
    
    /**
     * 退货物流单号
     */
    @Size(max = 100, message = "退货物流单号不能超过100字符")
    private String returnTrackingNumber;
    
    /**
     * 换货物流单号
     */
    @Size(max = 100, message = "换货物流单号不能超过100字符")
    private String replacementTrackingNumber;
    
    /**
     * 物流公司
     */
    @Size(max = 100, message = "物流公司名称不能超过100字符")
    private String logisticsCompany;
    
    /**
     * 质量检测报告文件路径
     */
    private String qualityReportPath;
    
    /**
     * 质量检测报告文件名称
     */
    private String qualityReportName;
    
    /**
     * 质量问题类型编码 (仅质量换货时使用)
     */
    private String qualityIssueCode;
    
    /**
     * 质量问题描述 (仅质量换货时使用)
     */
    @Size(max = 1000, message = "质量问题描述不能超过1000字符")
    private String qualityIssueDescription;
    
    /**
     * 影响程度
     */
    @Pattern(regexp = "minor|moderate|severe|critical", message = "影响程度必须是minor(轻微)、moderate(中等)、severe(严重)或critical(致命)")
    private String impactLevel;
    
    /**
     * 索赔金额
     */
    @DecimalMin(value = "0.00", message = "索赔金额不能为负数")
    private BigDecimal claimAmount = BigDecimal.ZERO;
    
    /**
     * 索赔状态
     */
    private String claimStatus = "pending";
    
    /**
     * 备注
     */
    @Size(max = 2000, message = "备注不能超过2000字符")
    private String remark;
    
    /**
     * 附件信息 (JSON格式存储附件列表)
     */
    private String attachments;
    
    /**
     * 换货单明细列表
     */
    @Valid
    @NotEmpty(message = "换货单明细不能为空")
    private List<PurchaseReturnItemDTO> items;
    
    /**
     * 质量换货特有字段 - 质量检测报告
     */
    @Data
    public static class QualityInspectionReport {
        private String reportPath;
        private String reportName;
        private String issueCode;
        private String issueDescription;
        private String impactLevel;
        private String inspector;
        private LocalDateTime inspectionDate;
        private String inspectionResult;
    }
}