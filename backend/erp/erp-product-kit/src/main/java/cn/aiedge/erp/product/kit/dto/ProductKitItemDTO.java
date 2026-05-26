package cn.aiedge.erp.product.kit.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductKitItemDTO {

    private Long componentProductId;

    private String componentProductCode;

    private String componentProductName;

    private String componentProductSpec;

    private String componentProductUnit;

    private BigDecimal quantity;

    private BigDecimal unitCost;

    private Boolean optional;

    private Boolean substitutable;

    private Long substituteProductId;

    private String substituteProductCode;

    private String remark;
}