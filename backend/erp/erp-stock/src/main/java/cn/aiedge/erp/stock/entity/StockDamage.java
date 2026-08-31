package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报损单（库存损耗/报废出库录单，按批次报损，单号前缀 BSD-）
 * 与报溢单互为反向单据（一损/一溢）。
 */
@Data
@Accessors(chain = true)
@TableName("erp_stock_damage")
public class StockDamage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String damageNo;
    private LocalDate damageDate;
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
    private Integer damageCause;
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
    private List<StockDamageItem> items;
}
