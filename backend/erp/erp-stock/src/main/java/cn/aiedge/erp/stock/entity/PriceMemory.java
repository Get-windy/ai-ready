package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_price_memory")
public class PriceMemory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String bizType;
    private Long productId;
    private Long partnerId;
    private BigDecimal unitPrice;
    private BigDecimal unitPriceExTax;
    private BigDecimal quantity;
    private BigDecimal totalAmount;
    private BigDecimal taxRate;
    private String currency;
    private Long orderId;
    private String orderNo;
    private LocalDate orderDate;
    private String priceSource;
    private Integer isLatest;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
