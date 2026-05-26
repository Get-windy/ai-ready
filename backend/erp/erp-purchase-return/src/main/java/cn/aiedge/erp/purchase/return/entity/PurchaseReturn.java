package cn.aiedge.erp.purchase.return.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 采购换货单实体类
 * 
 * 对应数据库表: purchase_return_order
 * 功能: 存储采购换货申请的基本信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("purchase_return_order")
public class PurchaseReturn {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 换货单号 (自动生成: RTN+年月日+8位序列号)
     */
    @TableField("return_code")
    private String returnCode;
    
    /**
     * 原采购订单号
     */
    @TableField("purchase_order_code")
    private String purchaseOrderCode;
    
    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    private Long supplierId;
    
    /**
     * 供应商名称
     */
    @TableField("supplier_name")
    private String supplierName;
    
    /**
     * 换货类型 (quality - 质量换货, quantity - 数量补货, specification - 规格换货)
     */
    @TableField("return_type")
    private String returnType;
    
    /**
     * 换货原因编码 (来自字典表)
     */
    @TableField("return_reason_code")
    private String returnReasonCode;
    
    /**
     * 换货原因描述
     */
    @TableField("return_reason")
    private String returnReason;
    
    /**
     * 期望处理方式 (return_and_replace - 退换货, replacement - 只换不退, return_and_refund - 退货退款)
     */
    @TableField("expected_solution")
    private String expectedSolution;
    
    /**
     * 换货状态 (draft - 草稿, submitted - 已提交, under_review - 审核中, approved - 已批准, rejected - 已拒绝, 
     *           supplier_confirmed - 供应商已确认, returning - 退货中, returned - 已退货, replacing - 换货中, 
     *           replaced - 已换货, completed - 已完成, cancelled - 已取消)
     */
    @TableField("status")
    private String status;
    
    /**
     * 当前审批节点
     */
    @TableField("current_approval_node")
    private String currentApprovalNode;
    
    /**
     * 审批流程实例ID (Activiti)
     */
    @TableField("process_instance_id")
    private String processInstanceId;
    
    /**
     * 换货申请日期
     */
    @TableField("apply_date")
    private LocalDateTime applyDate;
    
    /**
     * 期望完成日期
     */
    @TableField("expected_complete_date")
    private LocalDateTime expectedCompleteDate;
    
    /**
     * 实际完成日期
     */
    @TableField("actual_complete_date")
    private LocalDateTime actualCompleteDate;
    
    /**
     * 申请人ID
     */
    @TableField("applicant_id")
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    @TableField("applicant_name")
    private String applicantName;
    
    /**
     * 申请部门ID
     */
    @TableField("department_id")
    private Long departmentId;
    
    /**
     * 申请部门名称
     */
    @TableField("department_name")
    private String departmentName;
    
    /**
     * 紧急程度 (normal - 一般, urgent - 紧急, critical - 特急)
     */
    @TableField("urgency_level")
    private String urgencyLevel;
    
    /**
     * 优先级 (1-5, 1为最高)
     */
    @TableField("priority")
    private Integer priority;
    
    /**
     * 换货总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;
    
    /**
     * 运费
     */
    @TableField("shipping_fee")
    private BigDecimal shippingFee;
    
    /**
     * 处理费用
     */
    @TableField("handling_fee")
    private BigDecimal handlingFee;
    
    /**
     * 其他费用
     */
    @TableField("other_fee")
    private BigDecimal otherFee;
    
    /**
     * 费用分摊方式 (supplier - 供应商承担, buyer - 采购方承担, share - 双方分摊)
     */
    @TableField("fee_allocation_method")
    private String feeAllocationMethod;
    
    /**
     * 供应商承担比例 (0-100)
     */
    @TableField("supplier_share_ratio")
    private Integer supplierShareRatio;
    
    /**
     * 换货成本影响分析
     */
    @TableField("cost_impact_analysis")
    private String costImpactAnalysis;
    
    /**
     * 供应商沟通记录摘要
     */
    @TableField("supplier_communication_summary")
    private String supplierCommunicationSummary;
    
    /**
     * 供应商确认状态 (pending - 待确认, confirmed - 已确认, rejected - 已拒绝)
     */
    @TableField("supplier_confirmation_status")
    private String supplierConfirmationStatus;
    
    /**
     * 供应商确认时间
     */
    @TableField("supplier_confirmed_at")
    private LocalDateTime supplierConfirmedAt;
    
    /**
     * 供应商确认备注
     */
    @TableField("supplier_confirmation_note")
    private String supplierConfirmationNote;
    
    /**
     * 退货物流单号
     */
    @TableField("return_tracking_number")
    private String returnTrackingNumber;
    
    /**
     * 换货物流单号
     */
    @TableField("replacement_tracking_number")
    private String replacementTrackingNumber;
    
    /**
     * 物流公司
     */
    @TableField("logistics_company")
    private String logisticsCompany;
    
    /**
     * 质量检测报告文件路径
     */
    @TableField("quality_report_path")
    private String qualityReportPath;
    
    /**
     * 质量检测报告文件名称
     */
    @TableField("quality_report_name")
    private String qualityReportName;
    
    /**
     * 质量问题类型编码
     */
    @TableField("quality_issue_code")
    private String qualityIssueCode;
    
    /**
     * 质量问题描述
     */
    @TableField("quality_issue_description")
    private String qualityIssueDescription;
    
    /**
     * 影响程度 (minor - 轻微, moderate - 中等, severe - 严重, critical - 致命)
     */
    @TableField("impact_level")
    private String impactLevel;
    
    /**
     * 索赔金额
     */
    @TableField("claim_amount")
    private BigDecimal claimAmount;
    
    /**
     * 索赔状态 (pending - 待处理, approved - 已批准, rejected - 已拒绝, paid - 已支付)
     */
    @TableField("claim_status")
    private String claimStatus;
    
    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
    
    /**
     * 附件信息 (JSON格式存储附件列表)
     */
    @TableField("attachments")
    private String attachments;
    
    /**
     * 数据版本号 (用于乐观锁)
     */
    @Version
    @TableField("version")
    private Integer version;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 创建人ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;
    
    /**
     * 创建人姓名
     */
    @TableField(value = "created_by_name", fill = FieldFill.INSERT)
    private String createdByName;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 更新人ID
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
    
    /**
     * 更新人姓名
     */
    @TableField(value = "updated_by_name", fill = FieldFill.INSERT_UPDATE)
    private String updatedByName;
    
    /**
     * 删除标识 (0: 未删除, 1: 已删除)
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    
    /**
     * 租户ID (多租户支持)
     */
    @TableField("tenant_id")
    private String tenantId;
}