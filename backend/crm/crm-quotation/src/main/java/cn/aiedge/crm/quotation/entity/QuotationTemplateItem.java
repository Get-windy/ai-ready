package cn.aiedge.crm.quotation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_quotation_template_item")
public class QuotationTemplateItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long templateId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private BigDecimal defaultQuantity;

    private BigDecimal defaultPrice;

    private BigDecimal defaultDiscountRate;

    private Boolean optional;

    private String description;

    private String remark;

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