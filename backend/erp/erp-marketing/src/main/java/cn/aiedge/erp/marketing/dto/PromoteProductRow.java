package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 「我要推广 → 商品」Tab 行（对标 17 列，默认显示 15 列）：
 * 图片 / 商品货号 / 商品名称 / 品牌 / 规格 / 型号 / 产地 / 单位 / 库存 / 最近销售时间 / 新增时间 /
 * 零售价 / 批发价 / 最近分享时间 / 分享次数 / 浏览人数 / 浏览次数
 */
@Data
public class PromoteProductRow {

    private Long id;
    private String imageUrl;
    private String productCode;
    private String productName;
    private String brand;
    private String spec;
    private String model;
    private String origin;
    private String unit;
    /** 库存（Σ erp_stock.quantity） */
    private BigDecimal stock;
    /** 最近销售时间 */
    private LocalDateTime lastSaleTime;
    /** 新增时间 */
    private LocalDateTime createTime;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;

    /** 最近分享时间 */
    private LocalDateTime lastShareTime;
    /** 分享次数 */
    private Integer shareCount;
    /** 浏览人数 */
    private Integer viewerCount;
    /** 浏览次数 */
    private Integer viewCount;
}
