package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartDTO {
    private Long id;
    private String productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private Boolean checked;

    // ── 管理端（/api/v1/mall/cart/page）附加字段：全部由既有表派生，未新增数据库列 ──
    /** 会员ID（mall_cart.customer_id，即 shop_user.id） */
    private Long customerId;
    /** 会员昵称 / 公司名（shop_user.nickname / company_name 派生） */
    private String memberName;
    /** 会员登录名 / 手机号（shop_user.username / phone 派生） */
    private String memberAccount;
    /** 加入时间（mall_cart.create_time） */
    private LocalDateTime createTime;
    /** 商品编码（v_mall_product.product_code 派生；mall_cart.product_id 即商品编码） */
    private String productCode;
    /** 规格（v_mall_product.specification ← erp_product.spec） */
    private String specification;
    /** 单位（v_mall_product.unit_name ← erp_product.unit） */
    private String unitName;
    /** 小计（= mall_cart.subtotal，管理端列键为 totalPrice，此处做同值别名） */
    private BigDecimal totalPrice;
}
