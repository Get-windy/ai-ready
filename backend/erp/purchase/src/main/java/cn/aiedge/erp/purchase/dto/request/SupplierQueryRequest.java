package cn.aiedge.erp.purchase.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商查询请求DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "供应商查询请求")
public class SupplierQueryRequest {

    @Schema(description = "供应商编号")
    @Size(max = 50, message = "供应商编号长度不能超过50个字符")
    private String supplierNo;

    @Schema(description = "供应商名称")
    @Size(max = 200, message = "供应商名称长度不能超过200个字符")
    private String supplierName;

    @Schema(description = "供应商名称（模糊查询）")
    @Size(max = 200, message = "供应商名称长度不能超过200个字符")
    private String supplierNameLike;

    @Schema(description = "供应商类型")
    private String supplierType;

    @Schema(description = "供应商类型列表")
    private List<String> supplierTypes;

    @Schema(description = "供应商等级")
    private String supplierLevel;

    @Schema(description = "供应商等级列表")
    private List<String> supplierLevels;

    @Schema(description = "统一社会信用代码")
    @Size(max = 50, message = "统一社会信用代码长度不能超过50个字符")
    private String unifiedSocialCreditCode;

    @Schema(description = "联系人")
    @Size(max = 100, message = "联系人长度不能超过100个字符")
    private String contactPerson;

    @Schema(description = "联系电话")
    @Size(max = 50, message = "联系电话长度不能超过50个字符")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    private String contactEmail;

    @Schema(description = "供应商状态")
    private String status;

    @Schema(description = "供应商状态列表")
    private List<String> statuses;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "是否是战略性供应商")
    private Boolean isStrategic;

    @Schema(description = "是否是黑名单供应商")
    private Boolean isBlacklisted;

    @Schema(description = "是否暂停合作")
    private Boolean isSuspended;

    @Schema(description = "所属行业")
    @Size(max = 100, message = "所属行业长度不能超过100个字符")
    private String industry;

    @Schema(description = "地区")
    @Size(max = 100, message = "地区长度不能超过100个字符")
    private String region;

    @Schema(description = "标签")
    @Size(max = 50, message = "标签长度不能超过50个字符")
    private String tag;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "质量认证包含")
    @Size(max = 100, message = "质量认证查询长度不能超过100个字符")
    private String qualityCertificationContains;

    @Schema(description = "最小注册资本（万元）")
    private Double minRegisteredCapital;

    @Schema(description = "最大注册资本（万元）")
    private Double maxRegisteredCapital;

    @Schema(description = "最小员工人数")
    private Integer minEmployeeCount;

    @Schema(description = "最大员工人数")
    private Integer maxEmployeeCount;

    @Schema(description = "最小年营业额（万元）")
    private Double minAnnualRevenue;

    @Schema(description = "最大年营业额（万元）")
    private Double maxAnnualRevenue;

    @Schema(description = "合作开始日期范围（开始）")
    private LocalDateTime cooperationStartDateFrom;

    @Schema(description = "合作开始日期范围（结束）")
    private LocalDateTime cooperationStartDateTo;

    @Schema(description = "合作结束日期范围（开始）")
    private LocalDateTime cooperationEndDateFrom;

    @Schema(description = "合作结束日期范围（结束）")
    private LocalDateTime cooperationEndDateTo;

    @Schema(description = "建立日期范围（开始）")
    private LocalDateTime establishmentDateFrom;

    @Schema(description = "建立日期范围（结束）")
    private LocalDateTime establishmentDateTo;

    @Schema(description = "信用额度最小（元）")
    private Double minCreditLimit;

    @Schema(description = "信用额度最大（元）")
    private Double maxCreditLimit;

    @Schema(description = "信用额度使用率最小（%）")
    private Double minCreditUtilizationRate;

    @Schema(description = "信用额度使用率最大（%）")
    private Double maxCreditUtilizationRate;

    @Schema(description = "最近评估时间范围（开始）")
    private LocalDateTime lastEvaluatedTimeFrom;

    @Schema(description = "最近评估时间范围（结束）")
    private LocalDateTime lastEvaluatedTimeTo;

    @Schema(description = "创建时间范围（开始）")
    private LocalDateTime createdTimeFrom;

    @Schema(description = "创建时间范围（结束）")
    private LocalDateTime createdTimeTo;

    @Schema(description = "更新时间范围（开始）")
    private LocalDateTime updatedTimeFrom;

    @Schema(description = "更新时间范围（结束）")
    private LocalDateTime updatedTimeTo;

    @Schema(description = "排序字段", example = "createdTime", allowableValues = {
        "supplierNo", "supplierName", "supplierLevel", "status", 
        "createdTime", "updatedTime", "creditLimit", "annualRevenue"
    })
    private String sortBy = "createdTime";

    @Schema(description = "排序方向", example = "DESC", allowableValues = {"ASC", "DESC"})
    private String sortDirection = "DESC";

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "20")
    private Integer pageSize = 20;

    @Schema(description = "是否包含扩展属性", example = "false")
    private Boolean includeExtendedAttributes = false;

    @Schema(description = "是否包含评估统计", example = "false")
    private Boolean includeEvaluationStats = false;

    @Schema(description = "是否包含交易统计", example = "false")
    private Boolean includeTransactionStats = false;

    @Schema(description = "是否包含绩效指标", example = "false")
    private Boolean includePerformanceMetrics = false;
}