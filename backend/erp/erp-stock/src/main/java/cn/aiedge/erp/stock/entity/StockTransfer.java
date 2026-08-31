package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("erp_stock_transfer")
public class StockTransfer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String transferNo;

    /** 调拨方式：1同价调拨 2异价调拨 */
    private Integer transferType;

    @TableField("bill_date")
    private LocalDate billDate;

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    /** 成本金额 */
    private BigDecimal totalCostAmount;

    /** 调拨差额 = 调拨金额 - 成本金额 */
    private BigDecimal totalTransferDiff;

    /** 总重量(kg) */
    private BigDecimal totalWeight;

    /** 总体积(m³) */
    private BigDecimal totalVolume;

    private Integer status;

    /** 经手人 */
    private Long applicantId;

    private String applicantName;

    private String handlerName;

    private String sourceBillNo;

    private Long departmentId;

    private String departmentName;

    private String summary;

    private Integer attachment;

    private Integer printCount;

    private String posterName;

    private LocalDateTime posterTime;

    /** 制单人（冗余姓名便于检索） */
    private String createByName;

    private LocalDateTime applyTime;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private LocalDateTime updateTime;

    private Long executeBy;

    private LocalDateTime executeTime;

    private Integer totalItems;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;

    private String extInfo;

    /** 明细（非持久化，详情/创建时随主表返回） */
    @TableField(exist = false)
    private List<StockTransferItem> items;
}
