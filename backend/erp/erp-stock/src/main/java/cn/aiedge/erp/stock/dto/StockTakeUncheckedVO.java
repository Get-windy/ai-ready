package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 盘点单「未盘商品查询」行：商品主数据 + 库存数据
 */
@Data
public class StockTakeUncheckedVO {
    private Long productId;
    private String productCode;
    private String productName;
    private String productUnit;
    private String productSpec;
    private String model;
    private String origin;
    private String brand;
    private String region;
    private String location;
    private String barcode;
    /** 可用库存 */
    private BigDecimal availableStock;
    /** 账面库存 */
    private BigDecimal stockQuantity;
    /** 重量(kg) */
    private BigDecimal weight;
    /** 体积(m³) */
    private BigDecimal volume;
    private Long warehouseId;
    private String warehouseName;
}
