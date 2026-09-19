package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

import java.time.LocalDateTime;

/**
 * 促销活动（营销域视图，物理表 {@code erp_promotion_activity}）。
 *
 * <p>⚠️ 同一张物理表在销售域另有 {@code cn.aiedge.erp.sale.entity.PromotionActivity}（Odoo 风格字段集，
 * 供 {@code /api/sale/promotion} 与分析域统计使用）。本实体是**营销域字段集**，由「商品促销 / 整单促销 / 特价」
 * 三页共用，用 {@code type} 作页间判别：PRODUCT 商品促销 / ORDER 整单促销 / SPECIAL_PRICE 特价。
 * 不另建营销域促销表（同类型业务表全系统只此一张）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("erp_promotion_activity")
public class PromoActivity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 活动名称 */
    private String name;
    /** 促销规则（对标「促销规则」列，逐字描述规则原文） */
    private String description;
    /** 促销方式：PRODUCT 商品促销 / ORDER 整单促销 / SPECIAL_PRICE 特价 */
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 促销商品 id 串（逗号分隔，"查看商品" 用） */
    private String productIds;
    /** 组合促销：1 是 / 0 否 */
    private Integer comboPromo;
    /** 促销类型（按商品数量 / 按商品金额 …） */
    private String promoType;
    /** 促销模式（满赠 / 满减 / 打折 / 特价 …） */
    private String promoMode;
    /** 使用范围：线下使用 / 线上线下 / 商城使用 */
    private String promoScope;
    /** 促销客户显示串（按客户等级或指定客户时的展示文案） */
    private String customerLevels;
    /** 促销客户 id 串（"查看客户" 用） */
    private String customerIds;

    /** 状态：published 促销中 / draft 未开始 / expired 已结束 / cancelled 已停用 */
    private String status;
    /** 制单人姓名 */
    private String creatorName;

    // ── 促销引擎结构化配置（V11.378.0） ──

    /** 折扣率（0-1，促销模式=打折 时使用；如 0.9 = 9 折） */
    private BigDecimal discountRate;
    /** 门槛金额（促销模式=满减 / 按商品金额 时使用） */
    private BigDecimal minAmount;
    /** 满减金额（促销模式=满减 时使用） */
    private BigDecimal reductionAmount;
    /** 优先级（数值越大越先算） */
    private Integer priority;
    /** 叠加策略：STACK 可叠加 / EXCLUSIVE 独占 */
    private String stackPolicy;
    /** 本单最大优惠（封顶） */
    private BigDecimal maxDiscountAmount;
    /** 特价单价（促销方式=特价 时使用） */
    private BigDecimal promoPrice;
    /** 活动总次数上限（NULL=不限） */
    private Integer quotaTotal;
    /** 活动已用次数 */
    private Integer quotaUsed;
    /** 每客户次数上限（NULL=不限） */
    private Integer quotaPerCustomer;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
