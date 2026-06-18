package cn.aiedge.erp.sale.outbound.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleOutboundVO {

    private Long id;

    private String outboundNo;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private LocalDate outboundDate;

    private Integer outboundType;

    private String outboundTypeDesc;

    private Integer status;

    private String statusDesc;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private Long warehouseId;

    private String warehouseName;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String shippingAddress;

    private String receiverName;

    private String receiverPhone;

    private String trackingNumber;

    private String logisticsCompany;

    private LocalDateTime expectedShipTime;

    private LocalDateTime actualShipTime;

    private Long pickingBy;

    private LocalDateTime pickingTime;

    private Long packingBy;

    private LocalDateTime packingTime;

    private Long shippedBy;

    private LocalDateTime shippedTime;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long completedBy;

    private LocalDateTime completedTime;

    private String remark;

    private String internalNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private List<?> items;
}