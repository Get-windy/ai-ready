package cn.aiedge.order.dto;

import cn.aiedge.order.entity.OrderItem;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单DTO
 */
@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private Long tenantId;
    private Long customerId;
    private String customerName;
    private Integer orderType;
    private String orderTypeName;
    private Integer status;
    private String statusName;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal unpaidAmount;
    private Long saleId;
    private String saleName;
    private Integer paymentMethod;
    private Integer deliveryMethod;
    private String remark;
    private String auditRemark;
    private LocalDateTime orderTime;
    private LocalDateTime auditTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime paymentTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 订单明细
    private List<OrderItemDTO> items;
}