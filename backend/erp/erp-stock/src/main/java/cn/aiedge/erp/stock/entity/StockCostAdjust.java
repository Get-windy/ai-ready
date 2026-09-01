package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成本调价单（库存商品成本单价调整录单，单号前缀 CBTJD-）
 * 仅调成本不动数量：记帐后库存成本单价由调前成本价改为调后成本价。
 */
@Data
@Accessors(chain = true)
@TableName("erp_stock_cost_adjust")
public class StockCostAdjust {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String adjustNo;

    private Integer adjustType;

    private LocalDate adjustDate;

    private Long warehouseId;

    private String warehouseName;

    private Long handlerId;

    private String handlerName;

    private Long deptId;

    private String deptName;

    private BigDecimal totalAdjustAmount;

    private Integer totalItems;

    private String reasonType;

    private String reasonDesc;

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

    @Version
    private Integer versionNo;

    @TableField(exist = false)
    private List<StockCostAdjustItem> items;
}
