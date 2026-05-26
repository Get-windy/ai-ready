package cn.aiedge.erp.purchase.dto;

import cn.aiedge.erp.purchase.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单DTO
 */
@Data
public class PurchaseOrderDTO {
    
    private Long contractId;
    
    private String orderNo;
    
    private LocalDateTime orderDate;
    
    private String supplierCode;
    
    private String supplierName;
    
    private BigDecimal totalAmount;
    
    private OrderStatus status;
}
