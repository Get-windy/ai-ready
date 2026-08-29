package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_assemble")
public class StockAssemble {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String assembleNo;

    private LocalDateTime assembleDate;

    private Long warehouseId;

    private String warehouseName;

    /** 成品入库仓库 */
    private Long inWarehouseId;

    private String inWarehouseName;

    /** 原料出库仓库 */
    private Long outWarehouseId;

    private String outWarehouseName;

    /** 经手人 */
    private String handlerName;

    /** 生产单位 */
    private String produceUnit;

    /** 打印次数 */
    private Integer printCount;

    /** 打印记录 */
    private String printRecords;

    private Long bomId;

    private String bomNo;

    private String bomName;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private BigDecimal assembleQuantity;

    private BigDecimal subTotalCost;

    private BigDecimal assembleFee;

    private BigDecimal totalCost;

    private BigDecimal outputQuantity;

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
