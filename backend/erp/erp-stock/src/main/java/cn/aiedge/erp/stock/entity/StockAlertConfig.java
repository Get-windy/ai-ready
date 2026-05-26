package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_alert_config")
public class StockAlertConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long productId;

    private String productCode;

    private String productName;

    private Long warehouseId;

    private String warehouseName;

    private BigDecimal minStock;

    private BigDecimal maxStock;

    private BigDecimal safetyStock;

    private Integer alertType;

    private Boolean enableLowStockAlert;

    private Boolean enableOverStockAlert;

    private Boolean enableExpiryAlert;

    private Integer expiryAlertDays;

    private String alertReceiver;

    private String alertEmail;

    private String alertPhone;

    private Integer alertFrequency;

    private Boolean active;

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