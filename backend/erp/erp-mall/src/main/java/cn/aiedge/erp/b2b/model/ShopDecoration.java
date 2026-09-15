package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城装修配置（首页 / 分类页 / 商品详情）。
 *
 * <p>对应表 {@code shop_decoration}（Flyway V11.366.0__Add_Mall_Shop_Decoration_Storage.sql）。</p>
 *
 * <p><b>⚠️ 表结构为本实现口径，不是对标实测结论：</b>对标 ql361「商城装修」只实测到
 * 4 个 Tab 与「排版布局/拖拽编辑器」的存在，<b>装修配置的后端字段/JSON 结构均未实测</b>。
 * 本表按「能承载前端已有的装修结构」设计，{@link #configJson} 按**不透明 JSON 文本**存取
 * （TEXT，不用 jsonb，无需 TypeHandler），后端不校验其内部形状 —— 前端传来什么就存什么。</p>
 *
 * <p>约定：{@code tenant_id} / {@code create_by} 等审计列由本模块 {@link BaseEntity} +
 * MetaObjectHandler 自动填充；租户隔离由 MyBatis-Plus 租户拦截器自动注入
 * （{@code shop_decoration} 不在 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 名单内）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_decoration")
public class ShopDecoration extends BaseEntity {

    // ── scope 取值（⚠️ 本实现口径；对标抓取只实测到 4 个 Tab，无 scope 编码）──
    /** 首页装修（对标 Tab 二「装修模板」的首页排版布局） */
    public static final String SCOPE_HOME = "HOME";
    /** 分类页装修（对标 Tab 三「分类页装修」） */
    public static final String SCOPE_CATEGORY = "CATEGORY";
    /** 商品详情装修（对标 Tab 四「商品详情」的富文本） */
    public static final String SCOPE_PRODUCT_DETAIL = "PRODUCT_DETAIL";

    /** 装修范围：HOME 首页 / CATEGORY 分类页 / PRODUCT_DETAIL 商品详情（本实现口径） */
    private String scope;

    /** 引用的模板 id（可空，逻辑关联 shop_template.id，不建外键） */
    private Long templateId;

    /** 装修名称（对标「我的模板」卡片名 / 「新增模板」的名称） */
    private String name;

    /**
     * 装修结构 JSON（PG 列 TEXT，**不用 jsonb**，实体直接映射 String，无需 TypeHandler）。
     *
     * <p>⚠️ 拖拽排版结构未实测，后端按不透明文本存取，不做结构校验。</p>
     */
    private String configJson;

    /** 1启用 0停用 */
    private Integer status;
}
