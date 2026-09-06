package cn.aiedge.wms.pick.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拣货明细行 VO（按明细分页，合并单据级字段）
 */
@Data
public class PickTaskDetailVO {

    // ── 明细级 ──
    private Long id;
    private Long taskId;
    private Integer lineNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private String productUnit;
    private Long locationId;
    private String locationCode;
    private BigDecimal expectedQuantity;
    private BigDecimal pickedQuantity;
    private BigDecimal shortageQuantity;
    private String batchNo;
    private String serialNo;
    private Integer status;
    private String remark;

    // ── 单据级 ──
    private String taskNo;
    private Integer sourceType;
    private String sourceOrderNo;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;
    private Integer taskStatus;
    private BigDecimal totalQuantity;
    private BigDecimal pickedTotalQuantity;
    private LocalDateTime createTime;
}
