package cn.aiedge.crm.quotation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_quotation_template")
public class QuotationTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String templateCode;

    private String templateName;

    private String description;

    private Integer templateType;

    private Long customerId;

    private String customerName;

    private Long productCategoryId;

    private String categoryName;

    private BigDecimal defaultDiscountRate;

    private BigDecimal defaultTaxRate;

    private String paymentTerms;

    private Integer paymentDays;

    private String deliveryTerms;

    private Integer deliveryDays;

    private String termsAndConditions;

    private String footerNote;

    private Boolean active;

    private Integer usageCount;

    private Long lastUsedBy;

    private LocalDateTime lastUsedTime;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}