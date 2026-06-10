package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 产品关联(推荐/替代/配件)
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_related")
public class ProductRelated {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long productId;

    private Long relatedProductId;

    private String relationType;

    private Integer sortOrder;

    private String remark;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String relatedProductCode;

    @TableField(exist = false)
    private String relatedProductName;

    @TableField(exist = false)
    private String relatedProductSpec;
}
