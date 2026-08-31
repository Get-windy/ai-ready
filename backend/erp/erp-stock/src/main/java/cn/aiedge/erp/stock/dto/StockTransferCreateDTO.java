package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StockTransferCreateDTO {

    /** 调拨方式：1同价调拨 2异价调拨 */
    private Integer transferType;

    private Long fromWarehouseId;
    private String fromWarehouseName;

    private Long toWarehouseId;
    private String toWarehouseName;

    /** 经手人 */
    private Long applicantId;
    private String applicantName;
    private String handlerName;

    private Long departmentId;
    private String departmentName;

    private LocalDate billDate;

    private String sourceBillNo;

    private String summary;

    private String remark;

    private Integer attachment;

    private Integer printCount;

    private String posterName;
    private java.time.LocalDateTime posterTime;

    private String createByName;

    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal totalCostAmount;
    private BigDecimal totalTransferDiff;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    private List<StockTransferItemDTO> items;
}
