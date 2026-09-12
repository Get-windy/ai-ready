package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商品授权-屏蔽客户关系
 *
 * <p>商品列表「商品授权」子标签的数据主体：某个商品对指定客户/区域屏蔽。</p>
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_shield")
public class ProductShield {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 商品ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    /** 屏蔽客户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long partnerId;

    /** 屏蔽客户名称 */
    private String partnerName;

    /** 屏蔽级别 ALL=完全屏蔽 CONSULT=需询价 HIDDEN_PRICE=隐藏价格 */
    private String shieldLevel;

    /** 屏蔽区域 */
    private String region;

    /** 备注 */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ── 非持久化字段（列表展示，来自 erp_product） ──

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productCodeAlias;

    @TableField(exist = false)
    private String imageUrl;

    @TableField(exist = false)
    private String barcode;

    @TableField(exist = false)
    private String spec;

    @TableField(exist = false)
    private String model;

    @TableField(exist = false)
    private String origin;

    @TableField(exist = false)
    private String brand;
}
