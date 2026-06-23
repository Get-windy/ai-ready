package cn.aiedge.erp.purchase.inbound.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseInboundVO {

    private Long id;

    private String inboundNo;

    private Long orderId;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private Long contractId;

    private String contractNo;

    private LocalDate inboundDate;

    private Integer inboundType;

    private String inboundTypeDesc;

    private Integer status;

    private String statusDesc;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmountWithTax;

    private Long warehouseId;

    private String warehouseName;

    private Long purchaserId;

    private String purchaserName;

    private Long departmentId;

    private String departmentName;

    private String trackingNumber;

    private String logisticsCompany;

    private LocalDateTime expectedArrivalTime;

    private LocalDateTime actualArrivalTime;

    private Long receivedBy;

    private LocalDateTime receivedTime;

    private Long qualityCheckedBy;

    private LocalDateTime qualityCheckedTime;

    private String qualityCheckResult;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long warehouseConfirmedBy;

    private LocalDateTime warehouseConfirmedTime;

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