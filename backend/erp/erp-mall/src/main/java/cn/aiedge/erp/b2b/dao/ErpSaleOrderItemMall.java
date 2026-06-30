package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * erp_sale_order_item 表映射（商城订单明细）
 * 替代 mall_order_item 表，消除数据冗余
 */
@Data
@TableName("erp_sale_order_item")
public class ErpSaleOrderItemMall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
