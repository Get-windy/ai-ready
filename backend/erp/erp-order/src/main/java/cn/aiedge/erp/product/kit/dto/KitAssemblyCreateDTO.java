package cn.aiedge.erp.product.kit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class KitAssemblyCreateDTO {

    private Long kitId;

    private String kitCode;

    private String kitName;

    private Long productId;

    private String productCode;

    private String productName;

    private LocalDate assemblyDate;

    private BigDecimal assemblyQuantity;

    private Long warehouseId;

    private String warehouseName;

    private String remark;

    private String internalNote;
}