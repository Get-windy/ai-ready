package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商报价实体
 * 记录供应商对采购需求的报价信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("purchase_supplier_quotation")
public class SupplierQuotation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 报价编号
     */
    @TableField("quotation_no")
    private String quotationNo;

    /**
     * 采购需求ID
     */
    @TableField("demand_id")
    private Long demandId;

    /**
     * 询价邀请ID
     */
    @TableField("invitation_id")
    private Long invitationId;

    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    private String supplierId;

    /**
     * 供应商名称
     */
    @TableField("supplier_name")
    private String supplierName;

    /**
     * 物料编码
     */
    @TableField("material_code")
    private String materialCode;

    /**
     * 物料名称
     */
    @TableField("material_name")
    private String materialName;

    /**
     * 物料规格
     */
    @TableField("material_specification")
    private String materialSpecification;

    /**
     * 物料品牌
     */
    @TableField("material_brand")
    private String materialBrand;

    /**
     * 物料型号
     */
    @TableField("material_model")
    private String materialModel;

    /**
     * 单位
     */
    @TableField("unit")
    private String unit;

    // ================ 价格信息 ================
    
    /**
     * 单价
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 货币
     */
    @TableField("currency")
    private String currency;

    /**
     * 税率（%）
     */
    @TableField("tax_rate")
    private BigDecimal taxRate;

    /**
     * 含税单价
     */
    @TableField("unit_price_with_tax")
    private BigDecimal unitPriceWithTax;

    /**
     * 数量
     */
    @TableField("quantity")
    private BigDecimal quantity;

    /**
     * 总金额（不含税）
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 税额
     */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    /**
     * 含税总金额
     */
    @TableField("total_amount_with_tax")
    private BigDecimal totalAmountWithTax;

    /**
     * 折扣率（%）
     */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /**
     * 折扣金额
     */
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    /**
     * 折后总金额
     */
    @TableField("final_amount")
    private BigDecimal finalAmount;

    /**
     * 价格有效期
     */
    @TableField("price_validity_period")
    private Integer priceValidityPeriod;

    /**
     * 价格有效期单位
     * DAY: 天, WEEK: 周, MONTH: 月
     */
    @TableField("price_validity_unit")
    private String priceValidityUnit;

    /**
     * 价格有效期至
     */
    @TableField("price_valid_until")
    private LocalDateTime priceValidUntil;

    // ================ 交货信息 ================
    
    /**
     * 交期（天数）
     */
    @TableField("delivery_days")
    private Integer deliveryDays;

    /**
     * 预计交货日期
     */
    @TableField("estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    /**
     * 交货地点
     */
    @TableField("delivery_location")
    private String deliveryLocation;

    /**
     * 运输方式
     */
    @TableField("transportation_method")
    private String transportationMethod;

    /**
     * 运费承担方
     * SUPPLIER: 供应商承担, BUYER: 采购方承担, SHARED: 共同承担
     */
    @TableField("freight_bearer")
    private String freightBearer;

    /**
     * 运费金额
     */
    @TableField("freight_amount")
    private BigDecimal freightAmount;

    /**
     * 保险费金额
     */
    @TableField("insurance_amount")
    private BigDecimal insuranceAmount;

    /**
     * 其他费用金额
     */
    @TableField("other_fees_amount")
    private BigDecimal otherFeesAmount;

    // ================ 付款信息 ================
    
    /**
     * 付款方式
     */
    @TableField("payment_method")
    private String paymentMethod;

    /**
     * 付款条件
     */
    @TableField("payment_terms")
    private String paymentTerms;

    /**
     * 预付款比例（%）
     */
    @TableField("advance_payment_ratio")
    private BigDecimal advancePaymentRatio;

    /**
     * 预付款金额
     */
    @TableField("advance_payment_amount")
    private BigDecimal advancePaymentAmount;

    /**
     * 账期（天数）
     */
    @TableField("credit_period")
    private Integer creditPeriod;

    /**
     * 质保金比例（%）
     */
    @TableField("warranty_deposit_ratio")
    private BigDecimal warrantyDepositRatio;

    /**
     * 质保金金额
     */
    @TableField("warranty_deposit_amount")
    private BigDecimal warrantyDepositAmount;

    /**
     * 质保期限（月）
     */
    @TableField("warranty_period")
    private Integer warrantyPeriod;

    // ================ 质量与服务信息 ================
    
    /**
     * 质量标准
     */
    @TableField("quality_standard")
    private String qualityStandard;

    /**
     * 检验标准
     */
    @TableField("inspection_standard")
    private String inspectionStandard;

    /**
     * 验收标准
     */
    @TableField("acceptance_standard")
    private String acceptanceStandard;

    /**
     * 包装要求
     */
    @TableField("packaging_requirements")
    private String packagingRequirements;

    /**
     * 售后服务承诺
     */
    @TableField("after_sales_service")
    private String afterSalesService;

    /**
     * 技术支持承诺
     */
    @TableField("technical_support")
    private String technicalSupport;

    /**
     * 培训服务承诺
     */
    @TableField("training_service")
    private String trainingService;

    // ================ 报价状态与评估 ================
    
    /**
     * 报价状态
     * DRAFT: 草稿, SUBMITTED: 已提交, UNDER_REVIEW: 评审中, 
     * ACCEPTED: 已接受, REJECTED: 已拒绝, EXPIRED: 已过期, 
     * WITHDRAWN: 已撤回, CONVERTED_TO_ORDER: 已转为订单
     */
    @TableField("quotation_status")
    private String quotationStatus;

    /**
     * 提交时间
     */
    @TableField("submitted_time")
    private LocalDateTime submittedTime;

    /**
     * 提交人ID
     */
    @TableField("submitter_id")
    private String submitterId;

    /**
     * 提交人姓名
     */
    @TableField("submitter_name")
    private String submitterName;

    /**
     * 评审状态
     */
    @TableField("review_status")
    private String reviewStatus;

    /**
     * 评审得分（0-100）
     */
    @TableField("review_score")
    private BigDecimal reviewScore;

    /**
     * 价格合理性评分
     */
    @TableField("price_rationality_score")
    private BigDecimal priceRationalityScore;

    /**
     * 交期可行性评分
     */
    @TableField("delivery_feasibility_score")
    private BigDecimal deliveryFeasibilityScore;

    /**
     * 质量可靠性评分
     */
    @TableField("quality_reliability_score")
    private BigDecimal qualityReliabilityScore;

    /**
     * 服务保障评分
     */
    @TableField("service_assurance_score")
    private BigDecimal serviceAssuranceScore;

    /**
     * 综合评分
     */
    @TableField("overall_score")
    private BigDecimal overallScore;

    /**
     * 排名
     */
    @TableField("ranking")
    private Integer ranking;

    /**
     * 是否推荐
     */
    @TableField("is_recommended")
    private Boolean isRecommended;

    /**
     * 推荐等级
     */
    @TableField("recommendation_level")
    private String recommendationLevel;

    /**
     * 选择状态
     * SELECTED: 已选中, RESERVE: 备选, REJECTED: 已淘汰
     */
    @TableField("selection_status")
    private String selectionStatus;

    /**
     * 选择理由
     */
    @TableField("selection_reason")
    private String selectionReason;

    /**
     * 淘汰原因
     */
    @TableField("rejection_reason")
    private String rejectionReason;

    // ================ 关联信息 ================
    
    /**
     * 关联的采购订单ID
     */
    @TableField("purchase_order_id")
    private Long purchaseOrderId;

    /**
     * 关联的采购订单编号
     */
    @TableField("purchase_order_no")
    private String purchaseOrderNo;

    /**
     * 报价文件路径
     */
    @TableField("quotation_file_path")
    private String quotationFilePath;

    /**
     * 技术文件路径
     */
    @TableField("technical_file_path")
    private String technicalFilePath;

    /**
     * 资质文件路径
     */
    @TableField("qualification_file_path")
    private String qualificationFilePath;

    /**
     * 样品信息
     */
    @TableField("sample_info")
    private String sampleInfo;

    // ================ 附加信息 ================
    
    /**
     * 特殊条款
     */
    @TableField("special_terms")
    private String specialTerms;

    /**
     * 备注
     */
    @TableField("remarks")
    private String remarks;

    /**
     * 供应商备注
     */
    @TableField("supplier_remarks")
    private String supplierRemarks;

    /**
     * 内部评审意见
     */
    @TableField("internal_review_notes")
    private String internalReviewNotes;

    /**
     * 谈判记录
     */
    @TableField("negotiation_records")
    private String negotiationRecords;

    /**
     * AI分析结果（JSON格式）
     */
    @TableField("ai_analysis_result")
    private String aiAnalysisResult;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 是否最新版本
     */
    @TableField("is_latest_version")
    private Boolean isLatestVersion;

    /**
     * 前版本报价ID
     */
    @TableField("previous_version_id")
    private Long previousVersionId;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 逻辑删除标志
     */
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}