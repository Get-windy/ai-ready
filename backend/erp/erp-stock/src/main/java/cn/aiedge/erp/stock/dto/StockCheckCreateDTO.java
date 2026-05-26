package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockCheckCreateDTO {

    private Integer checkType;

    private Long warehouseId;

    private String warehouseName;

    private LocalDateTime checkDate;

    private Long checkerId;

    private String checkerName;

    private Long supervisorId;

    private String supervisorName;

    private String remark;

    private List<StockCheckItemDTO> items;
}