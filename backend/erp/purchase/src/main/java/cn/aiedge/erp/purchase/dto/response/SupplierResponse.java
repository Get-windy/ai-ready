package cn.aiedge.erp.purchase.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商响应DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "供应商响应")
public class SupplierResponse {

    @Schema(description = "供应商ID")
    private Long id;

    @Schema(description = "供应商编号")
    private String supplierNo;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "供应商简称")
    private String shortName;

    @Schema(description = "供应商类型")
    private String supplierType;

    @Schema(description = "供应商类型名称")
    private String supplierTypeName;

    @Schema(description = "供应商等级")
    private String supplierLevel;

    @Schema(description = "供应商等级名称")
    private String supplierLevelName;

    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    @Schema(description = "营业执照号")
    private String businessLicenseNo;

    @Schema(description = "法定代表人")
    private String legalRepresentative;

    @Schema(description = "注册资本（万元）")
    private BigDecimal registeredCapital;

    @Schema(description = "注册地址")
    private String registeredAddress;

    @Schema(description = "经营地址")
    private String businessAddress;

    @Schema(description = "邮政编码")
    private String postalCode;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Schema(description = "传真")
    private String fax;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "主营业务")
    private String mainBusiness;

    @Schema(description = "员工人数")
    private Integer employeeCount;

    @Schema(description = "年营业额（万元）")
    private BigDecimal annualRevenue;

    @Schema(description = "成立日期")
    private LocalDateTime establishmentDate;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "质量认证")
    private String qualityCertifications;

    @Schema(description = "银行账户名称")
    private String bankAccountName;

    @Schema(description = "银行账号")
    private String bankAccountNo;

    @Schema(description = "开户银行")
    private String bankName;

    @Schema(description = "供应商状态")
    private String status;

    @Schema(description = "供应商状态名称")
    private String statusName;

    @Schema(description = "合作开始日期")
    private LocalDateTime cooperationStartDate;

    @Schema(description = "合作结束日期")
    private LocalDateTime cooperationEndDate;

    @Schema(description = "是否是战略性供应商")
    private Boolean isStrategic;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "是否黑名单")
    private Boolean isBlacklisted;

    @Schema(description = "是否暂停合作")
    private Boolean isSuspended;

    @Schema(description = "结算周期（天）")
    private Integer settlementPeriod;

    @Schema(description = "信用额度（元）")
    private BigDecimal creditLimit;

    @Schema(description = "当前信用额度使用（元）")
    private BigDecimal creditUsed;

    @Schema(description = "信用额度使用率（%）")
    private BigDecimal creditUtilizationRate;

    @Schema(description = "付款方式")
    private String paymentMethod;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "税率（%）")
    private BigDecimal taxRate;

    @Schema(description = "物流合作商")
    private String logisticsPartner;

    @Schema(description = "物流联系方式")
    private String logisticsContact;

    @Schema(description = "综合评分（0-100）")
    private BigDecimal overallScore;

    @Schema(description = "质量评分（0-100）")
    private BigDecimal qualityScore;

    @Schema(description = "价格评分（0-100）")
    private BigDecimal priceScore;

    @Schema(description = "交期评分（0-100）")
    private BigDecimal deliveryScore;

    @Schema(description = "服务评分（0-100）")
    private BigDecimal serviceScore;

    @Schema(description = "技术评分（0-100）")
    private BigDecimal technicalScore;

    @Schema(description = "信用评分（0-100）")
    private BigDecimal creditScore;

    @Schema(description = "风险评分（0-100，越高风险越大）")
    private BigDecimal riskScore;

    @Schema(description = "最近评估时间")
    private LocalDateTime lastEvaluatedTime;

    @Schema(description = "推荐等级")
    private String recommendationLevel;

    @Schema(description = "累计采购金额（元）")
    private BigDecimal totalPurchaseAmount;

    @Schema(description = "累计采购次数")
    private Integer totalPurchaseCount;

    @Schema(description = "平均交货周期（天）")
    private BigDecimal averageDeliveryDays;

    @Schema(description = "准时交货率（%）")
    private BigDecimal onTimeDeliveryRate;

    @Schema(description = "质量合格率（%）")
    private BigDecimal qualityPassRate;

    @Schema(description = "投诉次数")
    private Integer complaintCount;

    @Schema(description = "投诉解决率（%）")
    private BigDecimal complaintResolutionRate;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "扩展属性（JSON格式）")
    private String extendedAttributes;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "更新人ID")
    private Long updatedBy;

    @Schema(description = "创建人姓名")
    private String createdByName;

    @Schema(description = "更新人姓名")
    private String updatedByName;

    @Schema(description = "绩效评估统计")
    private PerformanceMetrics performanceMetrics;

    @Schema(description = "交易统计")
    private TransactionStats transactionStats;

    @Schema(description = "评估历史统计")
    private EvaluationStats evaluationStats;

    /**
     * 绩效指标
     */
    @Data
    @Schema(description = "绩效指标")
    public static class PerformanceMetrics {
        
        @Schema(description = "综合绩效等级")
        private String performanceGrade;
        
        @Schema(description = "质量绩效")
        private MetricDetail qualityPerformance;
        
        @Schema(description = "交付绩效")
        private MetricDetail deliveryPerformance;
        
        @Schema(description = "成本绩效")
        private MetricDetail costPerformance;
        
        @Schema(description = "服务绩效")
        private MetricDetail servicePerformance;
        
        @Schema(description = "技术绩效")
        private MetricDetail technicalPerformance;
        
        @Schema(description = "同比变化（%）")
        private BigDecimal yearOverYearChange;
        
        @Schema(description = "环比变化（%）")
        private BigDecimal quarterOverQuarterChange;
    }

    /**
     * 交易统计
     */
    @Data
    @Schema(description = "交易统计")
    public static class TransactionStats {
        
        @Schema(description = "今年采购金额（元）")
        private BigDecimal currentYearPurchaseAmount;
        
        @Schema(description = "去年采购金额（元）")
        private BigDecimal lastYearPurchaseAmount;
        
        @Schema(description = "今年采购次数")
        private Integer currentYearPurchaseCount;
        
        @Schema(description = "去年采购次数")
        private Integer lastYearPurchaseCount;
        
        @Schema(description = "平均订单金额（元）")
        private BigDecimal averageOrderAmount;
        
        @Schema(description = "最大订单金额（元）")
        private BigDecimal maxOrderAmount;
        
        @Schema(description = "最小订单金额（元）")
        private BigDecimal minOrderAmount;
        
        @Schema(description = "合作时长（月）")
        private Integer cooperationMonths;
    }

    /**
     * 评估统计
     */
    @Data
    @Schema(description = "评估统计")
    public static class EvaluationStats {
        
        @Schema(description = "总评估次数")
        private Integer totalEvaluations;
        
        @Schema(description = "最近评估结果")
        private String latestEvaluationResult;
        
        @Schema(description = "平均综合评分")
        private BigDecimal averageOverallScore;
        
        @Schema(description = "平均质量评分")
        private BigDecimal averageQualityScore;
        
        @Schema(description = "平均交期评分")
        private BigDecimal averageDeliveryScore;
        
        @Schema(description = "趋势（上升/下降/稳定）")
        private String trend;
        
        @Schema(description = "下次评估时间")
        private LocalDateTime nextEvaluationDate;
    }

    /**
     * 指标详情
     */
    @Data
    @Schema(description = "指标详情")
    public static class MetricDetail {
        
        @Schema(description = "得分（0-100）")
        private BigDecimal score;
        
        @Schema(description = "等级")
        private String grade;
        
        @Schema(description = "趋势")
        private String trend;
        
        @Schema(description = "关键指标值")
        private String keyValue;
        
        @Schema(description = "行业平均水平")
        private BigDecimal industryAverage;
        
        @Schema(description = "是否达标")
        private Boolean meetsStandard;
    }
}