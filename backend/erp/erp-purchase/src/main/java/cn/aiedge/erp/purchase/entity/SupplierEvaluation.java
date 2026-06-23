package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商评估实体
 * 记录对供应商的详细评估信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("purchase_supplier_evaluation")
public class SupplierEvaluation implements Serializable {

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
     * 评估编号
     */
    @TableField("evaluation_no")
    private String evaluationNo;

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
     * 评估类型
     * INITIAL: 初次评估, PERIODIC: 定期评估, 
     * SPECIAL: 专项评估, RE_EVALUATION: 重新评估
     */
    @TableField("evaluation_type")
    private String evaluationType;

    /**
     * 评估周期
     */
    @TableField("evaluation_period")
    private String evaluationPeriod;

    /**
     * 评估开始时间
     */
    @TableField("evaluation_start_time")
    private LocalDateTime evaluationStartTime;

    /**
     * 评估结束时间
     */
    @TableField("evaluation_end_time")
    private LocalDateTime evaluationEndTime;

    /**
     * 评估状态
     * DRAFT: 草稿, IN_PROGRESS: 进行中, 
     * COMPLETED: 已完成, APPROVED: 已批准
     */
    @TableField("evaluation_status")
    private String evaluationStatus;

    // ================ 质量维度评分 ================
    
    /**
     * 质量总评分（0-100）
     */
    @TableField("quality_total_score")
    private BigDecimal qualityTotalScore;

    /**
     * 产品质量评分
     */
    @TableField("product_quality_score")
    private BigDecimal productQualityScore;

    /**
     * 过程质量评分
     */
    @TableField("process_quality_score")
    private BigDecimal processQualityScore;

    /**
     * 质量控制评分
     */
    @TableField("quality_control_score")
    private BigDecimal qualityControlScore;

    /**
     * 质量改进评分
     */
    @TableField("quality_improvement_score")
    private BigDecimal qualityImprovementScore;

    /**
     * 缺陷率（%）
     */
    @TableField("defect_rate")
    private BigDecimal defectRate;

    /**
     * 退货率（%）
     */
    @TableField("return_rate")
    private BigDecimal returnRate;

    /**
     * 投诉次数
     */
    @TableField("complaint_count")
    private Integer complaintCount;

    /**
     * 质量认证数量
     */
    @TableField("quality_certification_count")
    private Integer qualityCertificationCount;

    // ================ 价格维度评分 ================
    
    /**
     * 价格总评分（0-100）
     */
    @TableField("price_total_score")
    private BigDecimal priceTotalScore;

    /**
     * 价格竞争力评分
     */
    @TableField("price_competitiveness_score")
    private BigDecimal priceCompetitivenessScore;

    /**
     * 价格稳定性评分
     */
    @TableField("price_stability_score")
    private BigDecimal priceStabilityScore;

    /**
     * 成本透明度评分
     */
    @TableField("cost_transparency_score")
    private BigDecimal costTransparencyScore;

    /**
     * 付款条件评分
     */
    @TableField("payment_terms_score")
    private BigDecimal paymentTermsScore;

    /**
     * 平均价格水平（与市场比较%）
     */
    @TableField("average_price_level")
    private BigDecimal averagePriceLevel;

    /**
     * 价格波动率（%）
     */
    @TableField("price_volatility")
    private BigDecimal priceVolatility;

    // ================ 交期维度评分 ================
    
    /**
     * 交期总评分（0-100）
     */
    @TableField("delivery_total_score")
    private BigDecimal deliveryTotalScore;

    /**
     * 准时交货率（%）
     */
    @TableField("on_time_delivery_rate")
    private BigDecimal onTimeDeliveryRate;

    /**
     * 平均交货天数
     */
    @TableField("average_delivery_days")
    private Integer averageDeliveryDays;

    /**
     * 交货稳定性评分
     */
    @TableField("delivery_stability_score")
    private BigDecimal deliveryStabilityScore;

    /**
     * 紧急交货能力评分
     */
    @TableField("emergency_delivery_score")
    private BigDecimal emergencyDeliveryScore;

    /**
     * 交货准确性评分
     */
    @TableField("delivery_accuracy_score")
    private BigDecimal deliveryAccuracyScore;

    /**
     * 物流能力评分
     */
    @TableField("logistics_capability_score")
    private BigDecimal logisticsCapabilityScore;

    // ================ 服务维度评分 ================
    
    /**
     * 服务总评分（0-100）
     */
    @TableField("service_total_score")
    private BigDecimal serviceTotalScore;

    /**
     * 响应速度评分
     */
    @TableField("response_speed_score")
    private BigDecimal responseSpeedScore;

    /**
     * 沟通能力评分
     */
    @TableField("communication_skill_score")
    private BigDecimal communicationSkillScore;

    /**
     * 问题解决评分
     */
    @TableField("problem_solving_score")
    private BigDecimal problemSolvingScore;

    /**
     * 技术支持评分
     */
    @TableField("technical_support_score")
    private BigDecimal technicalSupportScore;

    /**
     * 售后服务评分
     */
    @TableField("after_sales_service_score")
    private BigDecimal afterSalesServiceScore;

    /**
     * 客户满意度评分
     */
    @TableField("customer_satisfaction_score")
    private BigDecimal customerSatisfactionScore;

    // ================ 技术维度评分 ================
    
    /**
     * 技术总评分（0-100）
     */
    @TableField("technical_total_score")
    private BigDecimal technicalTotalScore;

    /**
     * 技术能力评分
     */
    @TableField("technical_capability_score")
    private BigDecimal technicalCapabilityScore;

    /**
     * 研发能力评分
     */
    @TableField("rd_capability_score")
    private BigDecimal rdCapabilityScore;

    /**
     * 创新能力评分
     */
    @TableField("innovation_capability_score")
    private BigDecimal innovationCapabilityScore;

    /**
     * 技术文档评分
     */
    @TableField("technical_documentation_score")
    private BigDecimal technicalDocumentationScore;

    /**
     * 技术培训评分
     */
    @TableField("technical_training_score")
    private BigDecimal technicalTrainingScore;

    // ================ 合作维度评分 ================
    
    /**
     * 合作总评分（0-100）
     */
    @TableField("cooperation_total_score")
    private BigDecimal cooperationTotalScore;

    /**
     * 合作历史评分
     */
    @TableField("cooperation_history_score")
    private BigDecimal cooperationHistoryScore;

    /**
     * 合作态度评分
     */
    @TableField("cooperation_attitude_score")
    private BigDecimal cooperationAttitudeScore;

    /**
     * 合同履约评分
     */
    @TableField("contract_performance_score")
    private BigDecimal contractPerformanceScore;

    /**
     * 信息共享评分
     */
    @TableField("information_sharing_score")
    private BigDecimal informationSharingScore;

    /**
     * 协同能力评分
     */
    @TableField("collaboration_capability_score")
    private BigDecimal collaborationCapabilityScore;

    // ================ 信用与风险维度 ================
    
    /**
     * 信用总评分（0-100）
     */
    @TableField("credit_total_score")
    private BigDecimal creditTotalScore;

    /**
     * 付款准时率（%）
     */
    @TableField("payment_on_time_rate")
    private BigDecimal paymentOnTimeRate;

    /**
     * 财务状况评分
     */
    @TableField("financial_status_score")
    private BigDecimal financialStatusScore;

    /**
     * 法律合规评分
     */
    @TableField("legal_compliance_score")
    private BigDecimal legalComplianceScore;

    /**
     * 社会声誉评分
     */
    @TableField("social_reputation_score")
    private BigDecimal socialReputationScore;

    /**
     * 风险总评分（0-100，越高风险越大）
     */
    @TableField("risk_total_score")
    private BigDecimal riskTotalScore;

    /**
     * 财务风险评分
     */
    @TableField("financial_risk_score")
    private BigDecimal financialRiskScore;

    /**
     * 运营风险评分
     */
    @TableField("operational_risk_score")
    private BigDecimal operationalRiskScore;

    /**
     * 市场风险评分
     */
    @TableField("market_risk_score")
    private BigDecimal marketRiskScore;

    /**
     * 供应链风险评分
     */
    @TableField("supply_chain_risk_score")
    private BigDecimal supplyChainRiskScore;

    // ================ 综合评分与等级 ================
    
    /**
     * 综合总评分（0-100）
     */
    @TableField("overall_total_score")
    private BigDecimal overallTotalScore;

    /**
     * 绩效等级
     * A: 优秀, B: 良好, C: 合格, D: 不合格
     */
    @TableField("performance_level")
    private String performanceLevel;

    /**
     * 风险等级
     * LOW: 低风险, MEDIUM: 中风险, HIGH: 高风险
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 推荐等级
     * STRONGLY_RECOMMEND: 强烈推荐, RECOMMEND: 推荐, 
     * NEUTRAL: 中性, NOT_RECOMMEND: 不推荐
     */
    @TableField("recommendation_level")
    private String recommendationLevel;

    /**
     * 合作建议
     * EXPAND: 扩大合作, MAINTAIN: 维持现状, 
     * REDUCE: 减少合作, TERMINATE: 终止合作
     */
    @TableField("cooperation_suggestion")
    private String cooperationSuggestion;

    // ================ 详细评估信息 ================
    
    /**
     * 优势分析
     */
    @TableField("strengths_analysis")
    private String strengthsAnalysis;

    /**
     * 劣势分析
     */
    @TableField("weaknesses_analysis")
    private String weaknessesAnalysis;

    /**
     * 改进建议
     */
    @TableField("improvement_suggestions")
    private String improvementSuggestions;

    /**
     * 关键风险点
     */
    @TableField("key_risk_points")
    private String keyRiskPoints;

    /**
     * 监控重点
     */
    @TableField("monitoring_focus")
    private String monitoringFocus;

    /**
     * 评估结论
     */
    @TableField("evaluation_conclusion")
    private String evaluationConclusion;

    /**
     * 评估人ID
     */
    @TableField("evaluator_id")
    private Long evaluatorId;

    /**
     * 评估人姓名
     */
    @TableField("evaluator_name")
    private String evaluatorName;

    /**
     * 批准人ID
     */
    @TableField("approver_id")
    private Long approverId;

    /**
     * 批准人姓名
     */
    @TableField("approver_name")
    private String approverName;

    /**
     * 批准时间
     */
    @TableField("approved_time")
    private LocalDateTime approvedTime;

    /**
     * AI分析结果（JSON格式）
     */
    @TableField("ai_analysis_result")
    private String aiAnalysisResult;

    /**
     * 评估报告文件路径
     */
    @TableField("report_file_path")
    private String reportFilePath;

    /**
     * 附件信息
     */
    @TableField("attachment_info")
    private String attachmentInfo;

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