package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 推荐商品关联
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_recommend")
public class ProductRecommend {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long productId;

    private Long recommendProductId;

    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ── 非持久化字段(用于前端展示) ──

    @TableField(exist = false)
    private String recommendProductCode;

    @TableField(exist = false)
    private String recommendProductName;

    @TableField(exist = false)
    private String recommendProductSpec;

    @TableField(exist = false)
    private String recommendProductUnit;

    @TableField(exist = false)
    private String recommendProductOrigin;

    @TableField(exist = false)
    private String recommendProductBrand;
}
