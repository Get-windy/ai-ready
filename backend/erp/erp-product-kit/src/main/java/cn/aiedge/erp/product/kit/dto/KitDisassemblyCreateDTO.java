package cn.aiedge.erp.product.kit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class KitDisassemblyCreateDTO {

    private Long kitId;

    private String kitCode;

    private String kitName;

    private Long productId;

    private String productCode;

    private String productName;

    private String batchNo;

    private LocalDate disassemblyDate;

    private BigDecimal disassemblyQuantity;

    private Long warehouseId;

    private String warehouseName;

    private String remark;

    private String internalNote;
}