package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 毛利分析
 */
@Data
@TableName("erp_profit_analysis")
public class ProfitAnalysis {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long orderId;
    private String orderNo;
    private Long productId;
    private String productName;
    private BigDecimal saleQuantity;
    private BigDecimal salePrice;
    private BigDecimal saleAmount;
    private BigDecimal costAmount;
    private BigDecimal grossProfit;
    private BigDecimal grossMargin;
    private LocalDate analysisDate;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
