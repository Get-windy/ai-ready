package cn.aiedge.erp.product.kit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_product_kit_item")
public class ProductKitItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long kitId;

    private Integer lineNo;

    private Long componentProductId;

    private String componentProductCode;

    private String componentProductName;

    private String componentProductSpec;

    private String componentProductUnit;

    private BigDecimal quantity;

    private BigDecimal unitCost;

    private BigDecimal lineCost;

    private Boolean optional;

    private Boolean substitutable;

    private Long substituteProductId;

    private String substituteProductCode;

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