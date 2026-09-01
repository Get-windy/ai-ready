package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_split")
public class StockSplit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String splitNo;

    private LocalDateTime splitDate;

    private Long warehouseId;

    private String warehouseName;

    /** 原料入库仓库 */
    private Long inWarehouseId;

    private String inWarehouseName;

    /** 成品出库仓库 */
    private Long outWarehouseId;

    private String outWarehouseName;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private Long deptId;

    private String deptName;

    /** 打印次数 */
    private Integer printCount;

    /** 打印记录 */
    private String printRecords;

    /** 摘要 */
    private String summary;

    /** 附件 */
    private String attachment;

    /** 记账人 */
    private Long bookkeeperId;

    private String bookkeeperName;

    /** 记账时间 */
    private LocalDateTime bookkeepingTime;

    /** 制单人 */
    private String creatorName;

    /** 总重量(kg) */
    private BigDecimal totalWeight;

    /** 总体积(m³) */
    private BigDecimal totalVolume;

    /** 取消原因 */
    private String cancelReason;

    /** 本单金额 */
    private BigDecimal totalCost;

    private Long bomId;

    private String bomNo;

    private String bomName;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private BigDecimal splitQuantity;

    private BigDecimal outputTotalCost;

    private BigDecimal subTotalCost;

    private Integer totalItems;

    private Integer status;

    private Long applicantId;

    private String applicantName;

    private LocalDateTime applyTime;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long executedBy;

    private LocalDateTime executedTime;

    private String remark;

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
}
