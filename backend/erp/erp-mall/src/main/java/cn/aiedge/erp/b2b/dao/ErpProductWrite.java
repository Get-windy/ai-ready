package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * erp_product 表「商城上架可写列」轻量写入映射（**只写不读**，勿用于查询）。
 *
 * <p>背景：商城商品上架页列表读 {@code v_mall_product} 视图（源表 {@code erp_product}），
 * 但编辑弹窗保存此前写已废弃的 {@code mall_product} 表（见 {@code V9.0.0__Trade_Center_Consolidation.sql}
 * Part 9：{@code COMMENT ON TABLE mall_product IS '[已废弃] 由 v_mall_product 视图替代,数据源为 erp_product'}），
 * 导致「保存后在列表看不到变化」。此类用于把弹窗提交的字段回写到权威表 {@code erp_product}。</p>
 *
 * <p>约束（与 {@link ErpProductMall} 的视图列一一对应，取值口径见 V11.361.6 视图定义）：
 * <ul>
 *   <li>{@code productName}   ← 视图 {@code product_name}</li>
 *   <li>{@code imageUrl}      ← 视图 {@code image_url}</li>
 *   <li>{@code retailPrice}   ← 视图 {@code sale_price}（视图即 {@code p.retail_price}，**不是** standard_price）</li>
 *   <li>{@code wholesalePrice}← 视图 {@code market_price} 与 {@code wholesale_price}（同源，均为 {@code p.wholesale_price}）</li>
 *   <li>{@code mallCategoryName} ← 视图 {@code category_name}（视图为 COALESCE(mall_category_name, category)）</li>
 *   <li>{@code mallShelfStatus}  ← 视图 {@code status}（1=ON_SHELF / 其余=INACTIVE）</li>
 *   <li>{@code mallDescription}  ← 视图 {@code description}</li>
 * </ul>
 * 未映射（视图派生列，无对应 {@code erp_product} 物理列，禁止硬写）：{@code stock_quantity}
 * （由 erp_stock 实时聚合）、{@code grade_price_N}（聚合 erp_product_unit）、{@code coupon_used}
 * （由 use_coupon 派生）、{@code customer_grade_codes}。</p>
 *
 * <p>列类型注意：{@code erp_product.create_by / update_by} 为 VARCHAR(50)（视图侧 CAST 为 BIGINT），
 * 故 {@code updateBy} 用 String 承载，避免向 varchar 列写 bigint 触发 42804。</p>
 *
 * <p>本类仅声明保存所必需的列，未声明列（product_code/unit/category/spec/status/价格等级等）
 * 一律不参与 SQL，防止误改商品主数据。</p>
 */
@Data
@TableName("erp_product")
public class ErpProductWrite implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（erp_product.id BIGSERIAL，与视图 v_mall_product.id 同源；仅用于 WHERE 定位） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户（仅用于 WHERE 归属校验，防止跨租户改写） */
    private Long tenantId;

    /** 商品名称（erp_product.product_name） */
    private String productName;

    /** 商品图片（erp_product.image_url） */
    private String imageUrl;

    /** 商城销售价 → erp_product.retail_price（视图 sale_price 的源列） */
    private BigDecimal retailPrice;

    /** 商城市场价 → erp_product.wholesale_price（视图 market_price / wholesale_price 的源列） */
    private BigDecimal wholesalePrice;

    /** 商城展示分类 → erp_product.mall_category_name（视图 category_name 的首选源列） */
    private String mallCategoryName;

    /** 商城上架状态 → erp_product.mall_shelf_status（1=上架 0=下架；**非** erp_product.status 显示状态） */
    private Integer mallShelfStatus;

    /** 商城详情描述 → erp_product.mall_description（视图 description 的源列） */
    private String mallDescription;

    /** 最后修改人（erp_product.update_by VARCHAR(50)） */
    private String updateBy;

    /** 最后修改时间（erp_product.update_time） */
    private LocalDateTime updateTime;

    /** 逻辑删除标记（erp_product.deleted；仅用于 WHERE deleted = 0 守卫） */
    private Integer deleted;
}
