package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_check_item")
public class StockCheckItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long checkId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long locationId;

    private String locationCode;

    private String batchNo;

    private BigDecimal bookQuantity;

    private BigDecimal actualQuantity;

    private BigDecimal diffQuantity;

    private Integer diffType;

    private BigDecimal unitCost;

    private BigDecimal bookAmount;

    private BigDecimal actualAmount;

    private BigDecimal diffAmount;

    private Integer checkStatus;

    private String checkNote;

    private LocalDateTime checkedTime;

    private Long checkedBy;

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