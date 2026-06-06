package cn.aiedge.erp.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商查询数据传输对象
 */
@Data
@Schema(description = "供应商查询数据传输对象")
public class SupplierQueryDTO {
    
    @Schema(description = "供应商ID列表")
    private List<Long> ids;
    
    @Schema(description = "供应商编码")
    private String supplierCode;
    
    @Schema(description = "供应商名称")
    private String supplierName;
    
    @Schema(description = "供应商简称")
    private String shortName;
    
    @Schema(description = "供应商类型列表")
    private List<Integer> supplierTypes;
    
    @Schema(description = "企业性质列表")
    private List<Integer> enterpriseNatures;
    
    @Schema(description = "统一社会信用代码")
    private String creditCode;
    
    @Schema(description = "联系人姓名")
    private String contactPerson;
    
    @Schema(description = "联系人电话")
    private String contactPhone;
    
    @Schema(description = "联系人邮箱")
    private String contactEmail;
    
    @Schema(description = "公司地址")
    private String companyAddress;
    
    @Schema(description = "合作状态列表")
    private List<Integer> cooperationStatuses;
    
    @Schema(description = "供应商等级列表")
    private List<String> supplierLevels;
    
    @Schema(description = "最小综合评分")
    private Double minComprehensiveScore;
    
    @Schema(description = "最大综合评分")
    private Double maxComprehensiveScore;
    
    @Schema(description = "认证状态列表")
    private List<Integer> certificationStatuses;
    
    @Schema(description = "门户账户状态列表")
    private List<Integer> portalStatuses;
    
    @Schema(description = "付款方式列表")
    private List<Integer> paymentMethods;
    
    @Schema(description = "运输方式列表")
    private List<Integer> transportMethods;
    
    @Schema(description = "发票类型列表")
    private List<Integer> invoiceTypes;
    
    @Schema(description = "分类标签")
    private List<String> categoryTags;
    
    @Schema(description = "创建时间范围开始")
    private LocalDateTime createTimeStart;
    
    @Schema(description = "创建时间范围结束")
    private LocalDateTime createTimeEnd;
    
    @ToString.Exclude
    @Schema(description = "更新时间范围开始")
    private LocalDateTime updateTimeStart;

    @ToString.Exclude
    @Schema(description = "更新时间范围结束")
    private LocalDateTime updateTimeEnd;
    
    @Schema(description = "创建人")
    private String createBy;
    
    @ToString.Exclude
    @Schema(description = "更新人")
    private String updateBy;
    
    @Schema(description = "关键词搜索（供应商编码、名称、简称、联系人等）")
    private String keyword;
    
    @Schema(description = "排序字段", example = "createTime")
    private String orderBy;
    
    @Schema(description = "排序方向：asc/desc", example = "desc")
    private String orderDirection;
    
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    
    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;
    
    @ToString.Exclude
    @Schema(description = "是否包含已删除数据", example = "false")
    private Boolean includeDeleted = false;
    
    @Schema(description = "是否包含禁用数据", example = "true")
    private Boolean includeDisabled = true;
}