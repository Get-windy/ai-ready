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

    private List<ProductKitItemDTO> items;
}