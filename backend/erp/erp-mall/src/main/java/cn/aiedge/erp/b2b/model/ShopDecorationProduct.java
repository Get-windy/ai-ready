package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品详情装修 - 应用商品关联（对标「商品详情 → 设置应用商品」）。
 *
 * <p>对应表 {@code shop_decoration_product}（Flyway V11.366.0__Add_Mall_Shop_Decoration_Storage.sql）。</p>
 *
 * <p><b>⚠️ 表结构为本实现口径，不是对标实测结论：</b>对标 ql361 只实测到「设置应用商品」
 * 这个**入口存在**，「交互（弹窗/多选/单选）」未实测。本表按「能承载前端已有的多选商品 id
 * 集合」设计，未造交互。一行 = 一个关联；写入口径为**按装修配置全量替换**。</p>
 *
 * <p>约定：审计列由 {@link BaseEntity} + MetaObjectHandler 自动填充；租户隔离由
 * MyBatis-Plus 租户拦截器自动注入。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_decoration_product")
public class ShopDecorationProduct extends BaseEntity {

    /** 装修配置 id（逻辑关联 shop_decoration.id，不建外键） */
    private Long decorationId;

    /** 商品 id（逻辑关联 erp_product.id，不建外键） */
    private Long productId;
}
