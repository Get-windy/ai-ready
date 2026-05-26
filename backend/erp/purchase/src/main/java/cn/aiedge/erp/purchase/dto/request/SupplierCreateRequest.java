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
 * 供应商创建请求DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "供应商创建请求")
public class SupplierCreateRequest {

    @Schema(description = "供应商编号", example = "SUP202605050001", required = true)
    @NotBlank(message = "供应商编号不能为空")
    @Size(max = 50, message = "供应商编号长度不能超过50个字符")
    private String supplierNo;

    @Schema(description = "供应商名称", example = "上海精密制造有限公司", required = true)
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 200, message = "供应商名称长度不能超过200个字符")
    private String supplierName;

    @Schema(description = "供应商简称", example = "上海精密")
    @Size(max = 100, message = "供应商简称长度不能超过100个字符")
    private String shortName;

    @Schema(description = "供应商类型", example = "MANUFACTURER", required = true)
    @NotBlank(message = "供应商类型不能为空")
    private String supplierType;

    @Schema(description = "供应商等级", example = "A")
    private String supplierLevel;

    @Schema(description = "统一社会信用代码", example = "91310115765432100A", required = true)
    @NotBlank(message = "统一社会信用代码不能为空")
    private String unifiedSocialCreditCode;

    @Schema(description = "营业执照号", example = "31000020240500001")
    private String businessLicenseNo;

    @Schema(description = "法定代表人", example = "张三")
    @Size(max = 100, message = "法定代表人长度不能超过100个字符")
    private String legalRepresentative;

    @Schema(description = "注册资本（万元）", example = "1000.00")
    private BigDecimal registeredCapital;

    @Schema(description = "注册地址", example = "上海市浦东新区张江高科技园区")
    @Size(max = 500, message = "注册地址长度不能超过500个字符")
    private String registeredAddress;

    @Schema(description = "经营地址", example = "上海市浦东新区张江高科技园区创业路100号")
    @Size(max = 500, message = "经营地址长度不能超过500个字符")
    private String businessAddress;

    @Schema(description = "邮政编码", example = "201203")
    @Size(max = 20, message = "邮政编码长度不能超过20个字符")
    private String postalCode;

    @Schema(description = "联系人", example = "李四")
    @NotBlank(message = "联系人不能为空")
    @Size(max = 100, message = "联系人长度不能超过100个字符")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    @NotBlank(message = "联系电话不能为空")
    @Size(max = 50, message = "联系电话长度不能超过50个字符")
    private String contactPhone;

    @Schema(description = "联系邮箱", example = "supplier@shanghaiprecision.com")
    @Size(max = 100, message = "联系邮箱长度不能超过100个字符")
    private String contactEmail;

    @Schema(description = "传真", example = "021-12345678")
    @Size(max = 50, message = "传真长度不能超过50个字符")
    private String fax;

    @Schema(description = "所属行业", example = "电子信息制造业")
    @Size(max = 100, message = "所属行业长度不能超过100个字符")
    private String industry;

    @Schema(description = "主营业务", example = "精密仪器制造、电子元器件生产")
    @Size(max = 500, message = "主营业务长度不能超过500个字符")
    private String mainBusiness;

    @Schema(description = "员工人数", example = "500")
    private Integer employeeCount;

    @Schema(description = "年营业额（万元）", example = "50000.00")
    private BigDecimal annualRevenue;

    @Schema(description = "成立日期", example = "2015-03-15T00:00:00")
    private LocalDateTime establishmentDate;

    @Schema(description = "经营范围", example = "精密仪器、电子设备、技术开发、技术转让、技术服务")
    @Size(max = 1000, message = "经营范围长度不能超过1000个字符")
    private String businessScope;

    @Schema(description = "质量认证", example = "ISO9001,ISO14001")
    @Size(max = 500, message = "质量认证长度不能超过500个字符")
    private String qualityCertifications;

    @Schema(description = "银行账户名称", example = "上海精密制造有限公司")
    @Size(max = 200, message = "银行账户名称长度不能超过200个字符")
    private String bankAccountName;

    @Schema(description = "银行账号", example = "6228480012345678901")
    @Size(max = 50, message = "银行账号长度不能超过50个字符")
    private String bankAccountNo;

    @Schema(description = "开户银行", example = "中国工商银行上海张江支行")
    @Size(max = 200, message = "开户银行长度不能超过200个字符")
    private String bankName;

    @Schema(description = "供应商状态", example = "PENDING_VERIFICATION", required = true)
    @NotBlank(message = "供应商状态不能为空")
    private String status;

    @Schema(description = "合作开始日期", example = "2024-01-01T00:00:00")
    private LocalDateTime cooperationStartDate;

    @Schema(description = "合作结束日期", example = "2026-12-31T00:00:00")
    private LocalDateTime cooperationEndDate;

    @Schema(description = "是否是战略性供应商", example = "false")
    private Boolean isStrategic = false;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;

    @Schema(description = "结算周期（天）", example = "30")
    private Integer settlementPeriod = 30;

    @Schema(description = "信用额度（元）", example = "500000.00")
    private BigDecimal creditLimit;

    @Schema(description = "当前信用额度使用（元）", example = "125000.00")
    private BigDecimal creditUsed;

    @Schema(description = "付款方式", example = "BANK_TRANSFER")
    private String paymentMethod;

    @Schema(description = "币种", example = "CNY")
    private String currency = "CNY";

    @Schema(description = "税率（%）", example = "13.00")
    private BigDecimal taxRate;

    @Schema(description = "物流合作商", example = "顺丰快递")
    @Size(max = 200, message = "物流合作商长度不能超过200个字符")
    private String logisticsPartner;

    @Schema(description = "物流联系方式", example = "400-811-1111")
    @Size(max = 50, message = "物流联系方式长度不能超过50个字符")
    private String logisticsContact;

    @Schema(description = "备注", example = "优质供应商，产品质量稳定，交期准时")
    @Size(max = 2000, message = "备注长度不能超过2000个字符")
    private String remarks;

    @Schema(description = "扩展属性（JSON格式）")
    private String extendedAttributes;

    @Schema(description = "标签列表")
    private List<String> tags;
}