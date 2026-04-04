package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单DTO
 */
@Data
public class SaleOrderDTO {

    private Long id;
    private Long tenantId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private LocalDateTime orderDate;
    private LocalDateTime expectedShipDate;
    private Integer status;
    private String statusName;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmountWithTax;
    private BigDecimal receivedAmount;
    private Long salesmanId;
    private String salesmanName;
    private Long deptId;
    private Long warehouseId;
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    /**
     * 订单明细列表
     */
    private List<SaleOrderItemDTO> items;
}