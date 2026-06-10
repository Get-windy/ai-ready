package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_partner")
public class Partner {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String partnerCode;
    private String partnerName;
    private String partnerShortName;
    private String partnerType;
    private Long partnerCategoryId;
    private Long partnerGradeId;
    private String unifiedSocialCode;
    private String taxId;
    private String legalPerson;
    private BigDecimal registeredCapital;
    private String companyPhone;
    private String companyEmail;
    private String companyWebsite;
    private String industry;
    private String country;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String paymentTerms;
    private BigDecimal creditLimit;
    private Integer creditDays;
    private BigDecimal taxRate;
    private String settleType;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private Long defaultWarehouseId;
    private Long defaultDeliveryAddrId;
    private String sourceChannel;
    private Long sourcePartnerId;
    private LocalDateTime firstOrderTime;
    private LocalDateTime lastOrderTime;
    private Integer totalOrderCount;
    private BigDecimal totalOrderAmount;
    private String remark;
    private String status;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String gradeName;
}
