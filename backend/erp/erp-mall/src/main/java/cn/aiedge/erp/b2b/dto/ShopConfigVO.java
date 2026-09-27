package cn.aiedge.erp.b2b.dto;

import cn.aiedge.erp.b2b.model.ShopConfig;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 商城 C 端「店铺配置」下发对象（{@code GET /api/v1/mall/shop/config}）。
 *
 * <p><b>为什么必须是白名单、不能直接返回 {@link ShopConfig} 实体</b>：
 * {@code tenant_shop_config} 是 82 列的大宽表，其中混有**凭据类字段** ——
 * {@code miniapp_appsecret}（小程序密钥）、{@code miniapp_pay_mch_key} /
 * {@code b2b_pay_mch_key}（支付商户密钥）。这些一旦随"店铺配置"下发到浏览器，
 * 等于把支付/小程序凭据交给任何能打开商城页的人。
 * 用黑名单（排除几个字段）的做法在**加新列时会静默泄露**，所以这里用白名单：
 * 新增列默认不下发，必须显式加进来。</p>
 *
 * <p><b>字段分组</b>（与《商城App设计方案》§五 的落位表一一对应）：
 * ① 店铺主体与装修 ② 准入与价格 ③ 商品展示口径 ④ 交易与履约
 * ⑤ 注册设置 ⑥ 分类页与售后。每个字段在 C 端的具体消费点见设计文档。</p>
 *
 * <p><b>不下发清单（刻意排除）</b>：{@code miniappAppsecret}、{@code miniappPayMchId}、
 * {@code miniappPayMchKey}、{@code b2bPayMchId}、{@code b2bPayMchKey}、
 * {@code smsSignature}、{@code authDomain}（部署配置，前端不用）、
 * {@code watermarkImage}（仅图片水印，三期再定）以及全部审计列。</p>
 */
@Data
public class ShopConfigVO {

    // ── ① 店铺主体与装修 ──
    private String shopName;
    private String shopLogo;
    private String shopDesc;
    private String themeColor;
    private String bannerIds;
    private Long templateId;
    private String mainCategory;
    private String officialQrCode;
    private String mallQrCode;
    private String wechatLink;
    private String qualifications;
    private String contacts;

    // ── ② 营业与准入 ──
    private Integer shopEnabled;
    private String openTime;
    private String closeTime;
    /** 是否允许游客进店（ALLOW / 其它） */
    private String allowGuest;
    /** 游客是否可见价格（SHOW / 其它） */
    private String guestShowPrice;
    /** 是否对未认证买家隐藏等级信息 */
    private Integer buyerHideLevel;

    // ── ③ 商品展示口径 ──
    private String displayListFields;
    private String displayDetailFields;
    private Integer showSales;
    private String stockDisplay;
    private String outOfStockDisplay;
    private Integer quantityScale;
    private Integer enableSplitUnit;
    private Integer priceTrack;
    private Integer priceTrackWithUnit;
    private Integer enableRetailPrice;
    private Integer productAuthManage;

    // ── ④ 交易与履约 ──
    private BigDecimal minOrderAmount;
    private BigDecimal freeShippingAmount;
    private BigDecimal freightAmount;
    private String freightTemplate;
    private Integer enableLogistics;
    private String logisticsMethods;
    private Integer selfDelivery;
    private Integer enablePickup;
    private String pickupAddresses;
    private String paymentMethods;
    private String paymentScenes;
    private Long defaultWarehouseId;
    private Integer autoReceiveEnabled;
    private Integer autoReceiveDays;
    private String messageSubscribe;

    // ── ⑤ 注册设置 ──
    private Integer enableRegister;
    private Integer enableJoinApply;
    private Integer regAuditRequired;
    private Integer enableAutoAudit;
    private Long regDefaultGradeId;
    private String regDefaultCategory;
    private Integer regGiveCoupon;
    private String regGiveCoupons;
    private String registerAgreement;

    // ── ⑥ 分类页与售后 ──
    private String categoryDisplayMode;
    private String categoryDefaultSort;
    private String categoryStyle;
    private Integer categoryShowCount;
    private Integer enableMallCategory;
    private String returnConsignee;
    private String returnPhone;
    private String returnAddress;

    /**
     * 由实体按白名单构造。
     *
     * <p><b>刻意不用 MapStruct / BeanUtils.copyProperties</b>：字段是按白名单逐个手写的，
     * 一旦有人往实体加列，这里**不会**自动跟上下发 —— 这正是我们要的默认安全。</p>
     */
    public static ShopConfigVO from(ShopConfig c) {
        ShopConfigVO v = new ShopConfigVO();
        if (c == null) {
            return v;
        }
        // ①
        v.shopName = c.getShopName();
        v.shopLogo = c.getShopLogo();
        v.shopDesc = c.getShopDesc();
        v.themeColor = c.getThemeColor();
        v.bannerIds = c.getBannerIds();
        v.templateId = c.getTemplateId();
        v.mainCategory = c.getMainCategory();
        v.officialQrCode = c.getOfficialQrCode();
        v.mallQrCode = c.getMallQrCode();
        v.wechatLink = c.getWechatLink();
        v.qualifications = c.getQualifications();
        v.contacts = c.getContacts();
        // ②
        v.shopEnabled = c.getShopEnabled();
        v.openTime = c.getOpenTime();
        v.closeTime = c.getCloseTime();
        v.allowGuest = c.getAllowGuest();
        v.guestShowPrice = c.getGuestShowPrice();
        v.buyerHideLevel = c.getBuyerHideLevel();
        // ③
        v.displayListFields = c.getDisplayListFields();
        v.displayDetailFields = c.getDisplayDetailFields();
        v.showSales = c.getShowSales();
        v.stockDisplay = c.getStockDisplay();
        v.outOfStockDisplay = c.getOutOfStockDisplay();
        v.quantityScale = c.getQuantityScale();
        v.enableSplitUnit = c.getEnableSplitUnit();
        v.priceTrack = c.getPriceTrack();
        v.priceTrackWithUnit = c.getPriceTrackWithUnit();
        v.enableRetailPrice = c.getEnableRetailPrice();
        v.productAuthManage = c.getProductAuthManage();
        // ④
        v.minOrderAmount = c.getMinOrderAmount();
        v.freeShippingAmount = c.getFreeShippingAmount();
        v.freightAmount = c.getFreightAmount();
        v.freightTemplate = c.getFreightTemplate();
        v.enableLogistics = c.getEnableLogistics();
        v.logisticsMethods = c.getLogisticsMethods();
        v.selfDelivery = c.getSelfDelivery();
        v.enablePickup = c.getEnablePickup();
        v.pickupAddresses = c.getPickupAddresses();
        v.paymentMethods = c.getPaymentMethods();
        v.paymentScenes = c.getPaymentScenes();
        v.defaultWarehouseId = c.getDefaultWarehouseId();
        v.autoReceiveEnabled = c.getAutoReceiveEnabled();
        v.autoReceiveDays = c.getAutoReceiveDays();
        v.messageSubscribe = c.getMessageSubscribe();
        // ⑤
        v.enableRegister = c.getEnableRegister();
        v.enableJoinApply = c.getEnableJoinApply();
        v.regAuditRequired = c.getRegAuditRequired();
        v.enableAutoAudit = c.getEnableAutoAudit();
        v.regDefaultGradeId = c.getRegDefaultGradeId();
        v.regDefaultCategory = c.getRegDefaultCategory();
        v.regGiveCoupon = c.getRegGiveCoupon();
        v.regGiveCoupons = c.getRegGiveCoupons();
        v.registerAgreement = c.getRegisterAgreement();
        // ⑥
        v.categoryDisplayMode = c.getCategoryDisplayMode();
        v.categoryDefaultSort = c.getCategoryDefaultSort();
        v.categoryStyle = c.getCategoryStyle();
        v.categoryShowCount = c.getCategoryShowCount();
        v.enableMallCategory = c.getEnableMallCategory();
        v.returnConsignee = c.getReturnConsignee();
        v.returnPhone = c.getReturnPhone();
        v.returnAddress = c.getReturnAddress();
        return v;
    }

    /** 白名单字段名（供自检/文档用；任何不在其中的列都不会下发） */
    public static List<String> whitelist() {
        return Arrays.asList(
                "shopName", "shopLogo", "shopDesc", "themeColor", "bannerIds", "templateId",
                "mainCategory", "officialQrCode", "mallQrCode", "wechatLink", "qualifications", "contacts",
                "shopEnabled", "openTime", "closeTime", "allowGuest", "guestShowPrice", "buyerHideLevel",
                "displayListFields", "displayDetailFields", "showSales", "stockDisplay", "outOfStockDisplay",
                "quantityScale", "enableSplitUnit", "priceTrack", "priceTrackWithUnit", "enableRetailPrice",
                "productAuthManage",
                "minOrderAmount", "freeShippingAmount", "freightAmount", "freightTemplate", "enableLogistics",
                "logisticsMethods", "selfDelivery", "enablePickup", "pickupAddresses", "paymentMethods",
                "paymentScenes", "defaultWarehouseId", "autoReceiveEnabled", "autoReceiveDays", "messageSubscribe",
                "enableRegister", "enableJoinApply", "regAuditRequired", "enableAutoAudit", "regDefaultGradeId",
                "regDefaultCategory", "regGiveCoupon", "regGiveCoupons", "registerAgreement",
                "categoryDisplayMode", "categoryDefaultSort", "categoryStyle", "categoryShowCount",
                "enableMallCategory", "returnConsignee", "returnPhone", "returnAddress");
    }
}
