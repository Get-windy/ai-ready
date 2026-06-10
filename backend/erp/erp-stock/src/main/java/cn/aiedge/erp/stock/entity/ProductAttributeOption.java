package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 产品属性值选项
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_attribute_option")
public class ProductAttributeOption {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long attrDefId;

    private String optionValue;

    private String optionLabel;

    private String colorHex;

    private Integer sortOrder;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
