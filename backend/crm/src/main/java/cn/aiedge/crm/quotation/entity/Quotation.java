package cn.aiedge.crm.quotation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("crm_quotation")
public class Quotation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

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

    private Integer quotationType;

    private String title;

    private String description;

    private BigDecimal totalAmount;

    private BigDecimal discountRate;

    private BigDecimal discountAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal finalAmount;

    private String currency;

    private BigDecimal exchangeRate;

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

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}