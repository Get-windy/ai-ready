package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 积分兑换目录行（对标 13 列）：
 * 商品名称 / 货号 / 单位 / 兑换所需积分 / 规格 / 型号 / 产地 + 6 个价格列（默认隐藏）
 */
@Data
public class PointsExchangeRow {

    private Long id;
    private Long productId;
    /** 商品名称 */
    private String productName;
    /** 货号 */
    private String productCode;
    /** 单位 */
    private String unit;
    /** 兑换所需积分 */
    private BigDecimal exchangePoints;
    /** 规格 */
    private String spec;
    /** 型号 */
    private String model;
    /** 产地 */
    private String origin;

    /** 预设进价 */
    private BigDecimal presetPurchasePrice;
    /** 参考成本 */
    private BigDecimal referenceCost;
    /** 最近进价 */
    private BigDecimal recentPurchasePrice;
    /** 批发价 */
    private BigDecimal wholesalePrice;
    /** 零售价 */
    private BigDecimal retailPrice;
    /** 最低售价 */
    private BigDecimal minSalePrice;

    private Integer status;
    private Integer sort;
    private String remark;
}
