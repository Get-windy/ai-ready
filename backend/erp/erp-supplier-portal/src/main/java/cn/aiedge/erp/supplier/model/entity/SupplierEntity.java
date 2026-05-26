package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 供应商实体
 * 存储供应商基本信息，支持多租户数据隔离
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier")
public class SupplierEntity {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("short_name")
    private String shortName;

    @TableField("supplier_type")
    private Integer supplierType;

    @TableField("enterprise_nature")
    private Integer enterpriseNature;

    @TableField("credit_code")
    private String creditCode;

    @TableField("business_license")
    private String businessLicense;

    @TableField("legal_person")
    private String legalPerson;

    @TableField("registered_capital")
    private Double registeredCapital;

    @TableField("establishment_date")
    private LocalDateTime establishmentDate;

    @TableField("business_scope")
    private String businessScope;

    @TableField("contact_person")
    private String contactPerson;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("company_address")
    private String companyAddress;

    @TableField("postal_code")
    private String postalCode;

    @TableField("website")
    private String website;

    @TableField("bank_name")
    private String bankName;

    @TableField("bank_account")
    private String bankAccount;

    @TableField("tax_number")
    private String taxNumber;

    @TableField("invoice_type")
    private Integer invoiceType;

    @TableField("payment_method")
    private Integer paymentMethod;

    @TableField("payment_period")
    private Integer paymentPeriod;

    @TableField("transport_method")
    private Integer transportMethod;

    @TableField("cooperation_status")
    private Integer cooperationStatus;

    @TableField("supplier_level")
    private String supplierLevel;

    @TableField("comprehensive_score")
    private Double comprehensiveScore;

    @TableField("certification_status")
    private Integer certificationStatus;

    @TableField("portal_status")
    private Integer portalStatus;

    @TableField("portal_account_id")
    private String portalAccountId;

    @TableField("remark")
    private String remark;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @Version
    @TableField("version")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("category_tags")
    private String categoryTags;

    @TableField("location_info")
    private String locationInfo;

    @TableField("attachment_info")
    private String attachmentInfo;

    @TableField("extend_info")
    private String extendInfo;
}