package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城轮播图
 */
@Data
@TableName("shop_banner")
public class ShopBanner {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 标题 */
    private String title;

    /** 图片URL */
    private String imageUrl;

    /** 跳转链接 */
    private String linkUrl;

    /** product/category/none */
    private String linkType;

    /** 链接目标值(商品ID/分类ID) */
    private String linkValue;

    /** 排序 */
    private Integer sortOrder;

    /** 1启用 0停用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
