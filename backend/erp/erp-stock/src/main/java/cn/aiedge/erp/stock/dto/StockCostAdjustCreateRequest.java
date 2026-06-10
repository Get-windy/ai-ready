package cn.aiedge.erp.stock.dto;

import cn.aiedge.erp.stock.entity.StockCostAdjustItem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockCostAdjustCreateRequest {

    private Integer adjustType;

    private LocalDateTime adjustDate;

    private Long warehouseId;

    private String warehouseName;

    private String reasonType;

    private String reasonDesc;

    private String remark;

    private List<StockCostAdjustItem> items;
}
