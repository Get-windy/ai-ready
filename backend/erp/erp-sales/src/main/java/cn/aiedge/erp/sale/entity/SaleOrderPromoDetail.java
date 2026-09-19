package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单优惠分摊明细（促销引擎产出）：一条优惠 × 一个商品行 = 一行。
 *
 * <p>用途：① 让「本单到底享受了哪些优惠、各摊到哪个商品行」可回溯；
 * ② 为分析域「营销活动分析 / 毛利影响」提供**数据源**（此前只有金额合计、无分摊，无法算毛利影响）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_promo_detail")
public class SaleOrderPromoDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private Long orderId;
    private String orderNo;

    /** 促销活动 id（券优惠时为空） */
    private Long promotionId;
    private String promotionName;
    /** 打折 / 满减 / 特价 / 满赠 / 优惠券 */
    private String promoMode;
    /** ORDER 整单级 / ITEM 商品行级 */
    private String scopeType;

    private Integer lineNo;
    private Long productId;
    /** 该优惠在该行分摊到的优惠额 */
    private BigDecimal discountAmount;

    private Long couponId;
    private String couponCode;

    /** 满赠类：赠品信息（不计金额优惠） */
    private Long giftProductId;
    private BigDecimal giftQuantity;
    private String remark;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
