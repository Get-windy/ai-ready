package cn.aiedge.crm.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationCreateDTO {

    private Long customerId;

    private String customerName;

    private Long opportunityId;

    private String opportunityName;

    private Long contactId;

    private String contactName;

    private LocalDate quotationDate;

    private LocalDate validFrom;

    private LocalDate validTo;

    private Integer quotationType;

    private String title;

    private String description;

    private BigDecimal discountRate;

    private BigDecimal taxRate;

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

    private Integer winProbability;

    private List<QuotationItemDTO> items;
}