package cn.aiedge.erp.product.kit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductKitVO {

    private Long id;

    private String kitCode;

    private String kitName;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Integer kitType;

    private String kitTypeDesc;

    private Integer status;

    private BigDecimal kitPrice;

    private BigDecimal kitCost;

    private BigDecimal profitRate;

    private Boolean active;

    private Boolean allowSplit;

    private Boolean allowPartial;

    private Integer minQuantity;

    private Integer maxQuantity;

    private String description;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    /** 套餐图片 */
    private String imageUrl;

    /** 套餐条码 */
    private String barcode;

    /** 捆绑销售：1 是 / 0 否 */
    private Integer bundleSales;

    private List<?> items;
}