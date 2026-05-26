package cn.aiedge.erp.sale.return.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleReturnVO {

    private Long id;

    private String returnNo;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private LocalDate returnDate;

    private Integer returnType;

    private String returnTypeDesc;

    private String returnReason;

    private Integer status;

    private String statusDesc;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private BigDecimal refundAmount;

    private BigDecimal refundRate;

    private Long warehouseId;

    private String warehouseName;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long receivedBy;

    private LocalDateTime receivedTime;

    private Long warehouseConfirmedBy;

    private LocalDateTime warehouseConfirmedTime;

    private Long refundProcessedBy;

    private LocalDateTime refundProcessedTime;

    private String refundMethod;

    private String refundMethodDesc;

    private String refundAccount;

    private String refundNote;

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