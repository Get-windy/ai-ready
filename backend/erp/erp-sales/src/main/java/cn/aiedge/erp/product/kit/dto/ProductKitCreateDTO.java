package cn.aiedge.erp.product.kit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductKitCreateDTO {

    private String kitName;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Integer kitType;

    private BigDecimal kitPrice;

    private Boolean allowSplit;

    private Boolean allowPartial;

    private Integer minQuantity;

    private Integer maxQuantity;

    private String description;

    private String remark;

    /** 套餐图片（对标「套餐」页「图片」列） */
    private String imageUrl;

    /** 套餐条码（对标「套餐」页「套餐条码」列） */
    private String barcode;

    /** 捆绑销售：1 是 / 0 否 */
    private Integer bundleSales;

    private List<ProductKitItemDTO> items;
}