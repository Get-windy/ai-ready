package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_check")
public class StockCheck {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String checkNo;

    private Integer checkType;

    private Long warehouseId;

    private String warehouseName;

    private LocalDateTime checkDate;

    private Integer status;

    private Integer totalItems;

    private Integer checkedItems;

    private Integer diffItems;

    private BigDecimal totalBookQuantity;

    private BigDecimal totalActualQuantity;

    private BigDecimal totalDiffQuantity;

    private BigDecimal totalBookAmount;

    private BigDecimal totalActualAmount;

    private BigDecimal totalDiffAmount;

    private Long checkerId;

    private String checkerName;

    private Long supervisorId;

    private String supervisorName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long adjustedBy;

    private LocalDateTime adjustedTime;

    private String remark;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

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
}