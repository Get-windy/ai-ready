package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockTransferCreateDTO {

    private Integer transferType;

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;

    private LocalDateTime transferDate;

    private Long applicantId;

    private String applicantName;

    private Long departmentId;

    private String departmentName;

    private String remark;

    private List<StockTransferItemDTO> items;
}