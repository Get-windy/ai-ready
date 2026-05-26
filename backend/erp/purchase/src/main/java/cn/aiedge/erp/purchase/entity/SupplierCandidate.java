package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商候选实体
 * 记录采购需求匹配的供应商候选信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("purchase_supplier_candidate")
public class SupplierCandidate implements Serializable {

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
     * 采购需求ID
     */
    @TableField("demand_id")
    private Long demandId;

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
     * 供应商类别
     */
    @TableField("supplier_category")
    private String supplierCategory;

    /**
     * 供应商级别
     */
    @TableField("supplier_level")
    private String supplierLevel;

    /**
     * 所在地区
     */
    @TableField("region")
    private String region;

    /**
     * 匹配度评分（0-100）
     */
    @TableField("match_score")
    private BigDecimal matchScore;

    /**
     * 综合评分（0-100）
     */
    @TableField("overall_score")
    private BigDecimal overallScore;

    /**
     * 质量评分（0-100）
     */
    @TableField("quality_score")
    private BigDecimal qualityScore;

    /**
     * 价格评分（0-100）
     */
    @TableField("price_score")
    private BigDecimal priceScore;

    /**
     * 交期评分（0-100）
     */
    @TableField("delivery_score")
    private BigDecimal deliveryScore;

    /**
     * 服务评分（0-100）
     */
    @TableField("service_score")
    private BigDecimal serviceScore;

    /**
     * 技术评分（0-100）
     */
    @TableField("technical_score")
    private BigDecimal technicalScore;

    /**
     * 历史合作评分（0-100）
     */
    @TableField("cooperation_score")
    private BigDecimal cooperationScore;

    /**
     * 信用评分（0-100）
     */
    @TableField("credit_score")
    private BigDecimal creditScore;

    /**
     * 风险评分（0-100，越高风险越大）
     */
    @TableField("risk_score")
    private BigDecimal riskScore;

    /**
     * 推荐等级
     * A: 强烈推荐, B: 推荐, C: 一般, D: 不推荐
     */
    @TableField("recommendation_level")
    private String recommendationLevel;

    /**
     * 推荐理由
     */
    @TableField("recommendation_reason")
    private String recommendationReason;

    /**
     * 预估价格
     */
    @TableField("estimated_price")
    private BigDecimal estimatedPrice;

    /**
     * 价格单位
     */
    @TableField("price_unit")
    private String priceUnit;

    /**
     * 预估交期（天数）
     */
    @TableField("estimated_delivery_days")
    private Integer estimatedDeliveryDays;

    /**
     * 最小起订量
     */
    @TableField("min_order_quantity")
    private BigDecimal minOrderQuantity;

    /**
     * 产能情况
     */
    @TableField("capacity_status")
    private String capacityStatus;

    /**
     * 质量认证情况
     */
    @TableField("quality_certifications")
    private String qualityCertifications;

    /**
     * 特殊优势
     */
    @TableField("special_advantages")
    private String specialAdvantages;

    /**
     * 潜在风险
     */
    @TableField("potential_risks")
    private String potentialRisks;

    /**
     * 是否发送询价邀请
     */
    @TableField("invitation_sent")
    private Boolean invitationSent;

    /**
     * 询价邀请ID
     */
    @TableField("invitation_id")
    private Long invitationId;

    /**
     * 邀请发送时间
     */
    @TableField("invitation_sent_time")
    private LocalDateTime invitationSentTime;

    /**
     * 邀请状态
     * PENDING: 待发送, SENT: 已发送, ACCEPTED: 已接受, 
     * DECLINED: 已拒绝, EXPIRED: 已过期
     */
    @TableField("invitation_status")
    private String invitationStatus;

    /**
     * 是否已报价
     */
    @TableField("quotation_submitted")
    private Boolean quotationSubmitted;

    /**
     * 报价ID
     */
    @TableField("quotation_id")
    private Long quotationId;

    /**
     * 最终选择状态
     * SELECTED: 已选中, REJECTED: 已淘汰, PENDING: 待定
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

    /**
     * AI分析结果（JSON格式）
     */
    @TableField("ai_analysis_result")
    private String aiAnalysisResult;

    /**
     * 人工评审意见
     */
    @TableField("manual_review_notes")
    private String manualReviewNotes;

    /**
     * 是否黑名单供应商
     */
    @TableField("is_blacklisted")
    private Boolean isBlacklisted;

    /**
     * 是否暂停合作
     */
    @TableField("is_suspended")
    private Boolean isSuspended;

    /**
     * 数据来源
     * SYSTEM: 系统匹配, MANUAL: 手动添加, AI: AI推荐
     */
    @TableField("data_source")
    private String dataSource;

    /**
     * 匹配算法版本
     */
    @TableField("matching_algorithm_version")
    private String matchingAlgorithmVersion;

    /**
     * 匹配时间
     */
    @TableField("matched_time")
    private LocalDateTime matchedTime;

    /**
     * 最后评估时间
     */
    @TableField("last_evaluated_time")
    private LocalDateTime lastEvaluatedTime;

    /**
     * 备注
     */
    @TableField("remarks")
    private String remarks;

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