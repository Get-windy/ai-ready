package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租户商城配置
 */
@Data
@TableName("tenant_shop_config")
public class ShopConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 商城名称 */
    private String shopName;

    /** 商城LOGO */
    private String shopLogo;

    /** 商城描述 */
    private String shopDesc;

    /** 主题色 */
    private String themeColor;

    /** 轮播图ID列表(逗号分隔) */
    private String bannerIds;

    /** 所选页面模板ID */
    private Long templateId;

    /** 支付方式(逗号分隔) */
    private String paymentMethods;

    /** 是否开放注册 1=是 0=否 */
    private Integer enableRegister;

    /** 注册自动审核 1=是 0=否 */
    private Integer enableAutoAudit;

    /** 最小起订金额 */
    private BigDecimal minOrderAmount;

    /** 免运费金额 */
    private BigDecimal freeShippingAmount;

    /** 固定运费 */
    private BigDecimal freightAmount;

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
