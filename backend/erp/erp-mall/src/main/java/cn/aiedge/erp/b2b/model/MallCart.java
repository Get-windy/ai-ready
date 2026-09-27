package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城购物车行（{@code mall_cart}）。
 *
 * <p><b>2026-09-26 按真表结构修正三处</b>（真机验证时逐条暴露，都会让购物车接口 500）：</p>
 * <ol>
 *   <li>{@code customer_id} → 表里的真实列名是 <b>{@code user_id}</b>；
 *       <b>属性名仍叫 {@code customerId}</b>，因为服务层与 {@code CartDTO} 都用它，
 *       改列名即可、不动语义（映射用 {@code @TableField("user_id")}）。</li>
 *   <li>{@code subtotal} —— 表里**没有**这一列（由 单价×数量 派生）⇒ {@code @TableField(exist = false)}。</li>
 *   <li>{@code checked} —— 表里**没有**这一列（勾选状态不入库）⇒ {@code @TableField(exist = false)}。</li>
 * </ol>
 * <p>另：改为**不继承** {@link BaseEntity} —— 本表没有 {@code create_by / update_by}
 * （V9.34.0 建表即如此；全库 229/572 张表同样没有，故这两列不是全局约定），
 * 继承会把它们拼进 SELECT ⇒ {@code 字段 "create_by" 不存在}。字段按真表结构声明。</p>
 */
@Data
@TableName("mall_cart")
public class MallCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 归属买家（shop_user.id）；表列名 user_id，属性名沿用 customerId（服务层与 DTO 均用此名） */
    @TableField("user_id")
    private Long customerId;

    /** 商品编码（v_mall_product.product_id 口径） */
    private String productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    /** 行小计 = price × quantity（服务端维护；列由 V11.516.0 补上） */
    private BigDecimal subtotal;

    /**
     * 本行是否勾选。
     * ⚠️ 类型是 **Integer 0/1** 而不是 Boolean：列类型是 integer，
     *    而 PostgreSQL 不做 int↔boolean 隐式转换（同族表 mall_address.is_default 亦然）。
     *    对外 DTO 仍是 Boolean（前端语义），在 convertToDTO 里转换。
     */
    private Integer checked;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
