package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_transfer")
public class StockTransfer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String transferNo;

    private Integer transferType;

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;

    private LocalDateTime transferDate;

    private Integer status;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private Long applicantId;

    private String applicantName;

    private Long departmentId;

    private String departmentName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long executedBy;

    private LocalDateTime executedTime;

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