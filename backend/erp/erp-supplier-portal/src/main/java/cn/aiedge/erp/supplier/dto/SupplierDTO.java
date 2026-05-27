package cn.aiedge.erp.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 供应商数据传输对象
 */
@Data
@Schema(description = "供应商数据传输对象")
public class SupplierDTO {
    
    @Schema(description = "供应商ID", example = "1")
    private Long id;
    
    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 50, message = "供应商编码长度不能超过50个字符")
    @Schema(description = "供应商编码", example = "SUP20240001", required = true)
    private String supplierCode;
    
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 200, message = "供应商名称长度不能超过200个字符")
    @Schema(description = "供应商名称", example = "北京科技有限公司", required = true)
    private String supplierName;
    
    @Size(max = 100, message = "供应商简称长度不能超过100个字符")
    @Schema(description = "供应商简称", example = "北京科技")
    private String shortName;
    
    @NotNull(message = "供应商类型不能为空")
    @Schema(description = "供应商类型：1-生产商，2-代理商，3-经销商，4-服务商，5-其他", example = "1", required = true)
    private Integer supplierType;
    
    @Schema(description = "地址", example = "北京市朝阳区测试路123号")
    private String address;
    
    @Schema(description = "注册日期", example = "2020-01-01T00:00:00")
    private LocalDateTime registrationDate;
    
    @Schema(description = "状态：1-正常，0-禁用", example = "1")
    private Integer status;
    
    @Schema(description = "信用等级", example = "AAA")
    private String creditRating;
    
    @Schema(description = "绩效评分", example = "95.5")
    private Double performanceScore;
    
    @Schema(description = "备注")
    private String remarks;
    
    @Schema(description = "分类ID", example = "100")
    private Long categoryId;
    
    @Schema(description = "企业性质：1-国有企业，2-民营企业，3-外资企业，4-合资企业，5-其他", example = "2")
    private Integer enterpriseNature;
    
    @Size(max = 50, message = "统一社会信用代码长度不能超过50个字符")
    @Schema(description = "统一社会信用代码", example = "91110108712345678X")
    private String creditCode;
    
    @Size(max = 50, message = "营业执照号码长度不能超过50个字符")
    @Schema(description = "营业执照号码", example = "110105012345678")
    private String businessLicense;
    
    @Size(max = 50, message = "法定代表人长度不能超过50个字符")
    @Schema(description = "法定代表人", example = "张三")
    private String legalPerson;
    
    @Schema(description = "注册资本（万元）", example = "1000.0")
    private Double registeredCapital;
    
    @Schema(description = "成立日期", example = "2020-01-01T00:00:00")
    private LocalDateTime establishmentDate;
    
    @Schema(description = "经营范围")
    private String businessScope;
    
    @Size(max = 50, message = "联系人姓名长度不能超过50个字符")
    @Schema(description = "联系人姓名", example = "李四")
    private String contactPerson;
    
    @Size(max = 20, message = "联系人电话长度不能超过20个字符")
    @Schema(description = "联系人电话", example = "13800138000")
    private String contactPhone;
    
    @Size(max = 100, message = "联系人邮箱长度不能超过100个字符")
    @Schema(description = "联系人邮箱", example = "lisi@example.com")
    private String contactEmail;
    
    @Size(max = 500, message = "公司地址长度不能超过500个字符")
    @Schema(description = "公司地址", example = "北京市朝阳区建国门外大街1号")
    private String companyAddress;
    
    @Size(max = 10, message = "邮政编码长度不能超过10个字符")
    @Schema(description = "邮政编码", example = "100000")
    private String postalCode;
    
    @Size(max = 200, message = "公司网站长度不能超过200个字符")
    @Schema(description = "公司网站", example = "https://www.example.com")
    private String website;
    
    @Size(max = 100, message = "开户银行长度不能超过100个字符")
    @Schema(description = "开户银行", example = "中国工商银行北京分行")
    private String bankName;
    
    @Size(max = 50, message = "银行账号长度不能超过50个字符")
    @Schema(description = "银行账号", example = "6222021234567890123")
    private String bankAccount;
    
    @Size(max = 50, message = "纳税人识别号长度不能超过50个字符")
    @Schema(description = "纳税人识别号", example = "91110108712345678X")
    private String taxNumber;
    
    @Schema(description = "发票类型：1-增值税普通发票，2-增值税专用发票，3-其他", example = "2")
    private Integer invoiceType;
    
    @Schema(description = "付款方式：1-月结30天，2-月结60天，3-预付，4-货到付款，5-其他", example = "1")
    private Integer paymentMethod;
    
    @Schema(description = "付款账期（天）", example = "30")
    private Integer paymentPeriod;
    
    @Schema(description = "运输方式：1-自提，2-供应商送货，3-第三方物流，4-快递", example = "2")
    private Integer transportMethod;
    
    @Schema(description = "合作状态：1-潜在供应商，2-合格供应商，3-战略供应商，4-暂停合作，5-终止合作", example = "2")
    private Integer cooperationStatus;
    
    @Size(max = 10, message = "供应商等级长度不能超过10个字符")
    @Schema(description = "供应商等级：A/B/C/D", example = "A")
    private String supplierLevel;
    
    @Schema(description = "综合评分（0-100）", example = "85.5")
    private Double comprehensiveScore;
    
    @Schema(description = "认证状态：0-未认证，1-认证中，2-已认证，3-认证失败", example = "2")
    private Integer certificationStatus;
    
    @Schema(description = "门户账户状态：0-未激活，1-已激活，2-已禁用，3-已锁定", example = "1")
    private Integer portalStatus;
    
    @Size(max = 50, message = "门户账户ID长度不能超过50个字符")
    @Schema(description = "门户账户ID（关联用户系统）", example = "user123456")
    private String portalAccountId;
    
    @Schema(description = "备注信息")
    private String remark;
    
    @Schema(description = "供应商分类标签")
    private List<String> categoryTags;
    
    @Schema(description = "地理位置信息")
    private Map<String, Object> locationInfo;
    
    @Schema(description = "附件信息")
    private List<Map<String, Object>> attachmentInfo;
    
    @Schema(description = "扩展字段")
    private Map<String, Object> extendInfo;
    
    @Schema(description = "创建人", example = "admin")
    private String createBy;
    
    @Schema(description = "创建时间", example = "2024-01-01T00:00:00")
    private LocalDateTime createTime;
    
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    
    @Schema(description = "更新时间", example = "2024-01-01T00:00:00")
    private LocalDateTime updateTime;
}