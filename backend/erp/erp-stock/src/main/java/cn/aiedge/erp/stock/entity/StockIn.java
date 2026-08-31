package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 其他入库单（库存入库：盘盈/获赠/退货入库/其他）
 */
@Data
@Accessors(chain = true)
@TableName("erp_stock_in")
public class StockIn {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String stockInNo;
    private LocalDate stockInDate;
    private Integer stockInType;
    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    private Long warehouseId;
    private String warehouseName;
    private Long handlerId;
    private String handlerName;
    private Long deptId;
    private String deptName;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;
    private Integer totalItems;
    private Integer status;
    private String summary;
    private String remark;
    private String attachment;
    private Long bookkeeperId;
    private String bookkeeperName;
    private LocalDateTime bookkeepingTime;
    private String creatorName;
    private Integer printCount;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private String approvedNote;
    private Long executedBy;
    private LocalDateTime executedTime;
    private String cancelReason;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(exist = false)
    private List<StockInItem> items;
}
