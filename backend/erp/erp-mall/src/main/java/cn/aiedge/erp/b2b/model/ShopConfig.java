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

    // ========================================================================
    // 配置三页金标准列（Flyway V11.361.7__Add_Tenant_Shop_Config_GoldStandard_Fields.sql）
    // 页面：基础设置（basic-config）/ 店铺设置（shop-config）/ 运费设置（freight-config）
    //
    // 约定：
    //   · 布尔开关一律 Integer 0/1（PG 列为 INTEGER，用 Boolean 会 setBoolean 类型不匹配）
    //   · 字符串枚举 / URL 用 String，长文本与 JSON 结构化字段用 String（PG 为 TEXT，不用 jsonb）
    //   · 三页共用本单行实体 + 同一对 GET/PUT 端点；updateConfig 走 updateById，
    //     MyBatis-Plus 默认忽略 null 字段 → **部分字段提交不会覆盖其它字段**，三页可安全共存
    // ========================================================================

    // ── 基础设置页 ──
    /** 【基础设置】公众号二维码图片URL */
    private String officialQrCode;
    /** 【基础设置】主营类目 */
    private String mainCategory;
    /** 【基础设置】营业资质 JSON：{license,foodPermit,other1..other4} */
    private String qualifications;
    /** 【基础设置】联系方式 JSON 数组：[{type,number}] */
    private String contacts;
    /** 【基础设置】退货地址-收件人 */
    private String returnConsignee;
    /** 【基础设置】退货地址-收件电话 */
    private String returnPhone;
    /** 【基础设置】退货地址-收件地址 */
    private String returnAddress;
    /** 【基础设置】商品详情页显示字段 JSON 数组 */
    private String displayDetailFields;
    /** 【基础设置】商品列表页显示字段 JSON 数组 */
    private String displayListFields;
    /** 【基础设置】水印方式：none/image/text */
    private String watermarkType;
    /** 【基础设置】图片水印图片URL */
    private String watermarkImage;
    /** 【基础设置】文字水印文字 */
    private String watermarkText;
    /** 【基础设置】微商城二维码图片URL */
    private String mallQrCode;
    /** 【基础设置】微信版链接地址 */
    private String wechatLink;
    /** 【基础设置】微信公众号网页授权域名 */
    private String authDomain;
    /** 【基础设置】网店仓库（默认仓库ID） */
    private Long defaultWarehouseId;
    /** 【基础设置】小程序 appid（⚠️ 当前明文存储） */
    private String miniappAppid;
    /** 【基础设置】小程序 appsecret（⚠️ 当前明文存储） */
    private String miniappAppsecret;
    /** 【基础设置】小程序支付商户号（⚠️ 当前明文存储） */
    private String miniappPayMchId;
    /** 【基础设置】小程序支付商户密钥（⚠️ 当前明文存储） */
    private String miniappPayMchKey;
    /** 【基础设置】小程序商城地址 */
    private String miniappMallUrl;
    /** 【基础设置】门店助手B To B支付-支付商户号（⚠️ 当前明文存储） */
    private String b2bPayMchId;
    /** 【基础设置】门店助手B To B支付-支付商户密钥（⚠️ 当前明文存储） */
    private String b2bPayMchKey;
    /** 【基础设置】消息设置 JSON：16 项订阅开关 */
    private String messageSubscribe;

    // ── 店铺设置页 ──
    /** 【店铺设置】商城开关 1=开 0=关 */
    private Integer shopEnabled;
    /** 【店铺设置】营业时间-起（HH:mm） */
    private String openTime;
    /** 【店铺设置】营业时间-止（HH:mm） */
    private String closeTime;
    /** 【店铺设置】短信签名 */
    private String smsSignature;
    /** 【店铺设置】启用买家不显示客户级别 1=是 0=否 */
    private Integer buyerHideLevel;
    /** 【店铺设置】是否允许游客访问：NOT_ALLOW/ALLOW */
    private String allowGuest;
    /** 【店铺设置】游客显示价格：HIDE/SHOW */
    private String guestShowPrice;
    /** 【店铺设置】商城数量小数位数 0-3 */
    private Integer quantityScale;
    /** 【店铺设置】只允许微信登录 1=是 0=否 */
    private Integer wechatOnlyLogin;
    /** 【店铺设置】价格跟踪 1=是 0=否 */
    private Integer priceTrack;
    /** 【店铺设置】跟踪价格随单位联动 1=是 0=否（页面对应 priceTrackWithUnit） */
    private Integer priceTrackWithUnit;
    /** 【店铺设置】启用建议零售价 1=是 0=否 */
    private Integer enableRetailPrice;
    /** 【店铺设置】商品授权管理 1=是 0=否 */
    private Integer productAuthManage;
    /** 【店铺设置】启用分单位显示 1=是 0=否 */
    private Integer enableSplitUnit;
    /** 【店铺设置】启用商城显示销量 1=是 0=否 */
    private Integer showSales;
    /** 【店铺设置】库存显示方式：AVAILABLE/QUANTITY/HIDE */
    private String stockDisplay;
    /** 【店铺设置】启用商城分类 1=是 0=否 */
    private Integer enableMallCategory;
    /** 【店铺设置】无货商品显示方式：RESTOCKING/HIDE */
    private String outOfStockDisplay;
    /** 【店铺设置】启用 N 天自动收货 1=是 0=否 */
    private Integer autoReceiveEnabled;
    /** 【店铺设置】N 天自动收货天数 */
    private Integer autoReceiveDays;
    /**
     * 【店铺设置】⚠️ 已废弃（DEPRECATED，Flyway V11.365.0 起）。
     * 对标 ql361「店铺设置 → 注册设置」实测**没有「注册协议」**这一项，本字段为历史自造结构，
     * 前端「注册设置」子项**已不再读写**（见 V11.365.0 迁移注释理由：删列会破坏旧前端/已发布 JAR
     * 的 SQL 兼容性）。字段与既有数据仅为兼容保留，新代码禁止使用。
     */
    private String registerAgreement;
    /** 【店铺设置】支付场景矩阵 JSON 数组：[{code,enabled,ruleEnabled,ruleValue,sort}] */
    private String paymentScenes;

    // ── 店铺设置页 ·「注册设置」子项（对标实测 5 项，Flyway V11.365.0 重做）──
    // 对标实测（2026-09-14 ql361 活体抓取）该子项**只有 5 项且无「注册协议」**：
    //   ① 允许注册账号（复选框，同行「设置注册信息」按钮）
    //   ② 买家注册默认级别（必填下拉，同行「客户级别设置」按钮）
    //   ③ 买家注册默认分类（必填搜索式选择器）
    //   ④ 买家账号注册审核（必填下拉，否(0)/是(1)）
    //   ⑤ 新用户注册送优惠券（复选框，同行「设置优惠券」按钮）
    // 列名/取值口径见 V11.365.0__Rebuild_Mall_Shop_Register_Settings.sql。
    /** 【注册设置】① 允许注册账号 1=是 0=否（对标 EnableJoinApply / name=enable_join_apply） */
    private Integer enableJoinApply;
    /** 【注册设置】② 买家注册默认级别（必填，关联客户级别字典 PartnerGrade.id；对标 Default2bCustomerDealerTypeId） */
    private Long regDefaultGradeId;
    /** 【注册设置】③ 买家注册默认分类（必填；⚠️ 对标选项字典未实测，暂存原值字符串） */
    private String regDefaultCategory;
    /** 【注册设置】④ 买家账号注册审核 1=是 0=否（必填，对标取值 否(0)/是(1)） */
    private Integer regAuditRequired;
    /** 【注册设置】⑤ 新用户注册送优惠券 1=是 0=否（复选框） */
    private Integer regGiveCoupon;
    /** 【注册设置】⑤-1 优惠券设置 JSON：{opengive:boolean, couponslist:[]}（对标 MallRegGiveCoupons 实测形状） */
    private String regGiveCoupons;

    // ── 商城装修页 ·「分类页装修」子项（对标实测 4 项，Flyway V11.366.0）──
    // 对标实测（2026-09-07 ql361 抓取，见 docs/Yh-Spec/抓取结果/商城装修_抓取.json
    // 的 `Tab三_分类页装修.配置项`）该子项为 4 项：
    //   ① 分类展示方式     单选，实测「按目录分类」(勾选) / 「按品牌分类」
    //   ② 商品默认排序     下拉，实测默认「综合排序」（⚠️ 选项全集未实测）
    //   ③ 分类页分类样式   单选，实测「纯文本模式」(勾选) / 「图文模式」
    //   ④ 显示分类下商品数量 复选框，实测默认开启
    // 取值口径见 V11.366.0__Add_Mall_Shop_Decoration_Storage.sql。
    /** 【分类页装修】① 分类展示方式：CATALOG 按目录分类 / BRAND 按品牌分类（⚠️ 存储编码未实测，本实现口径） */
    private String categoryDisplayMode;
    /** 【分类页装修】② 商品默认排序：COMPOSITE 综合排序（⚠️ 对标选项全集未实测，本实现只落库实测项，不做枚举校验） */
    private String categoryDefaultSort;
    /** 【分类页装修】③ 分类页分类样式：TEXT 纯文本模式 / IMAGE 图文模式（⚠️ 存储编码未实测，本实现口径） */
    private String categoryStyle;
    /** 【分类页装修】④ 显示分类下商品数量 1=显示 0=不显示（复选框，对标实测默认开启） */
    private Integer categoryShowCount;

    // ── 运费设置页 ──
    /** 【运费设置】自有配送 1=开 0=关 */
    private Integer selfDelivery;
    /** 【运费设置】启用物流 1=开 0=关 */
    private Integer enableLogistics;
    /**
     * 【运费设置】⚠️ 已废弃（DEPRECATED，Flyway V11.364.0 起）。
     * 对标 ql361「运费设置 → 物流」实测**没有**「物流方式」列表，本字段为历史自造结构
     * [{type:CITY|EXPRESS|SELF,name,enabled}]，前端已不再读写。
     * 列与既有数据仅为兼容而保留（删列会破坏旧版本前端/已发布 JAR 的 SQL 兼容性），新代码禁止使用。
     */
    private String logisticsMethods;
    /**
     * 【运费设置】地区运费表 JSON（对标实测重建，Flyway V11.364.0 起）：
     * {wftype:1|2|3, defaultFreight, regions:[{code,province,city,area,
     *  firstWeight,freight,addWeight,addFreight,startCount,addCount,freeFreight,
     *  amountTiers:[{min,max,freight,caninput}]}]}。
     * wftype=运费计算方式（1=按重量 2=按订单金额 3=按订单数量）；
     * regions 为三张表（按重量/按订单金额/按订单数量）的共同行集合，维度=地区（省/市/区）；
     * freeFreight=满额包邮(元)，**每行一个金额**；defaultFreight=未设置运费地区的默认运费。
     */
    private String freightTemplate;
    /** 【运费设置】到店自提 1=开 0=关 */
    private Integer enablePickup;
    /** 【运费设置】提货地址 JSON 数组：[{contact,phone,address}] */
    private String pickupAddresses;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
