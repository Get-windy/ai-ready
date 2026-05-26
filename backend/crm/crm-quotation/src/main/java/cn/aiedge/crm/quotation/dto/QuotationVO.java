package cn.aiedge.crm.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class QuotationVO {

    private Long id;

    private String quotationNo;

    private Integer version;

    private Long parentId;

    private Long customerId;

    private String customerName;

    private Long opportunityId;

    private String opportunityName;

    private Long contactId;

    private String contactName;

    private LocalDate quotationDate;

    private LocalDate validFrom;

    private LocalDate validTo;

    private Integer status;

    private String statusDesc;

    private Integer quotationType;

    private String quotationTypeDesc;

    private String title;

    private String description;

    private BigDecimal totalAmount;

    private BigDecimal discountRate;

    private BigDecimal discountAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal finalAmount;

    private String currency;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String paymentTerms;

    private Integer paymentDays;

    private String deliveryTerms;

    private Integer deliveryDays;

    private String deliveryAddress;

    private String receiverName;

    private String receiverPhone;

    private String remark;

    private String internalNote;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long sentBy;

    private LocalDateTime sentTime;

    private String sentMethod;

    private Long acceptedBy;

    private LocalDateTime acceptedTime;

    private String acceptedNote;

    private Long rejectedBy;

    private LocalDateTime rejectedTime;

    private String rejectedReason;

    private Long convertedBy;

    private LocalDateTime convertedTime;

    private Long orderId;

    private String orderNo;

    private Integer winProbability;

    private String competitorQuote;

    private BigDecimal competitorPrice;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Integer itemCount;

    private Boolean isExpired;
}