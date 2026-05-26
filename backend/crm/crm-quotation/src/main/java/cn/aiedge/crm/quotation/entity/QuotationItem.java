package cn.aiedge.crm.quotation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_quotation_item")
public class QuotationItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long quotationId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long productCategoryId;

    private String categoryName;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal costPrice;

    private BigDecimal discountRate;

    private BigDecimal discountAmount;

    private BigDecimal lineAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal lineTotal;

    private String description;

    private String remark;

    private Integer deliveryDays;

    private String warrantyTerms;

    private String serviceTerms;

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