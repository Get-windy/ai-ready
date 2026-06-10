package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Long locationId;
    private String locationCode;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private Integer damageCause;
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
