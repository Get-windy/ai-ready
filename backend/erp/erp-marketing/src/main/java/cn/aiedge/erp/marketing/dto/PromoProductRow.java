package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 促销活动「查看商品」行 */
@Data
public class PromoProductRow {

    private Long id;
    private String productCode;
    private String productName;
    private String spec;
    private String unit;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
}
