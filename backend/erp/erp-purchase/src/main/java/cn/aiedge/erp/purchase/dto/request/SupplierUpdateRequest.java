package cn.aiedge.erp.purchase.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商更新请求DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "供应商更新请求")
public class SupplierUpdateRequest {

    @Schema(description = "供应商名称", example = "上海精密制造股份有限公司", required = true)
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 200, message = "供应商名称长度不能超过200个字符")
    private String supplierName;

    @Schema(description = "供应商简称", example = "上海精密股份")
    @Size(max = 100, message = "供应商简称长度不能超过100个字符")
    private String shortName;

    @Schema(description = "供应商类型", example = "MANUFACTURER")
    private String supplierType;

    @Schema(description = "供应商等级", example = "A+")
    private String supplierLevel;

    @Schema(description = "统一社会信用代码", example = "91310115765432100B")
    private String unifiedSocialCreditCode;

    @Schema(description = "营业执照号", example = "31000020240500001")
    private String businessLicenseNo;

    @Schema(description = "法定代表人", example = "张四")
    @Size(max = 100, message = "法定代表人长度不能超过100个字符")
    private String legalRepresentative;

    @Schema(description = "注册资本（万元）", example = "1500.00")
    private BigDecimal registeredCapital;

    @Schema(description = "注册地址", example = "上海市浦东新区张江高科技园区创新路200号")
    @Size(max = 500, message = "注册地址长度不能超过500个字符")
    private String registeredAddress;

    @Schema(description = "经营地址", example = "上海市浦东新区张江高科技园区创业路200号")
    @Size(max = 500, message = "经营地址长度不能超过500个字符")
    private String businessAddress;

    @Schema(description = "邮政编码", example = "201204")
    @Size(max = 20, message = "邮政编码长度不能超过20个字符")
    private String postalCode;

    @Schema(description = "联系人", example = "李五")
    @Size(max = 100, message = "联系人长度不能超过100个字符")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138001")
    @Size(max = 50, message = "联系电话长度不能超过50个字符")
    private String contactPhone;

    @Schema(description = "联系邮箱", example = "supplier@shanghaiprecision.com.cn")
    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    private String contactEmail;

    @Schema(description = "传真", example = "021-87654321")
    @Size(max = 50, message = "传真长度不能超过50个字符")
    private String fax;

    @Schema(description = "所属行业", example = "高端装备制造业")
    @Size(max = 100, message = "所属行业长度不能超过100个字符")
    private String industry;

    @Schema(description = "主营业务", example = "高端精密仪器制造、工业自动化设备、智能制造解决方案")
    @Size(max = 500, message = "主营业务长度不能超过500个字符")
    private String mainBusiness;

    @Schema(description = "员工人数", example = "800")
    private Integer employeeCount;

    @Schema(description = "年营业额（万元）", example = "80000.00")
    private BigDecimal annualRevenue;

    @Schema(description = "成立日期", example = "2015-03-15T00:00:00")
    private LocalDateTime establishmentDate;

    @Schema(description = "经营范围", example = "精密仪器、高端装备、工业自动化设备、智能制造解决方案、技术开发")
    @Size(max = 1000, message = "经营范围长度不能超过1000个字符")
    private String businessScope;

    @Schema(description = "质量认证", example = "ISO9001:2015,ISO14001:2015,ISO45001")
    @Size(max = 500, message = "质量认证长度不能超过500个字符")
    private String qualityCertifications;

    @Schema(description = "银行账户名称", example = "上海精密制造股份有限公司")
    @Size(max = 200, message = "银行账户名称长度不能超过200个字符")
    private String bankAccountName;

    @Schema(description = "银行账号", example = "6228480012345678902")
    @Size(max = 50, message = "银行账号长度不能超过50个字符")
    private String bankAccountNo;

    @Schema(description = "开户银行", example = "中国建设银行上海张江支行")
    @Size(max = 200, message = "开户银行长度不能超过200个字符")
    private String bankName;

    @Schema(description = "供应商状态", example = "ACTIVE")
    private String status;

    @Schema(description = "合作开始日期", example = "2024-01-01T00:00:00")
    private LocalDateTime cooperationStartDate;

    @Schema(description = "合作结束日期", example = "2027-12-31T00:00:00")
    private LocalDateTime cooperationEndDate;

    @Schema(description = "是否是战略性供应商", example = "true")
    private Boolean isStrategic;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "结算周期（天）", example = "60")
    private Integer settlementPeriod;

    @Schema(description = "信用额度（元）", example = "1000000.00")
    private BigDecimal creditLimit;

    @Schema(description = "当前信用额度使用（元）", example = "300000.00")
    private BigDecimal creditUsed;

    @Schema(description = "付款方式", example = "BANK_TRANSFER")
    private String paymentMethod;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "税率（%）", example = "13.00")
    private BigDecimal taxRate;

    @Schema(description = "物流合作商", example = "德邦物流")
    @Size(max = 200, message = "物流合作商长度不能超过200个字符")
    private String logisticsPartner;

    @Schema(description = "物流联系方式", example = "400-830-5555")
    @Size(max = 50, message = "物流联系方式长度不能超过50个字符")
    private String logisticsContact;

    @Schema(description = "备注", example = "战略合作伙伴，产品质量卓越，技术支持响应快速")
    @Size(max = 2000, message = "备注长度不能超过2000个字符")
    private String remarks;

    @Schema(description = "扩展属性（JSON格式）")
    private String extendedAttributes;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "更新原因", example = "升级为战略合作伙伴，信用额度提升")
    @Size(max = 500, message = "更新原因长度不能超过500个字符")
    private String updateReason;
}