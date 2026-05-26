package cn.aiedge.crm.contract.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ContractCreateDTO {

    private String contractName;

    private Integer contractType;

    private Long customerId;

    private String customerName;

    private Long opportunityId;

    private String opportunityName;

    private Long quotationId;

    private String quotationNo;

    private Long contactId;

    private String contactName;

    private LocalDate signDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal contractAmount;

    private String currency;

    private String paymentTerms;

    private Integer paymentDays;

    private Integer paymentMethod;

    private String deliveryTerms;

    private Integer deliveryDays;

    private String warrantyTerms;

    private Integer warrantyMonths;

    private String serviceTerms;

    private Integer serviceMonths;

    private String title;

    private String description;

    private String remark;

    private String internalNote;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private Boolean autoRenewal;

    private Integer renewalNoticeDays;

    private Boolean needReview;

    private Integer riskLevel;

    private String riskNote;

    private List<ContractClauseDTO> clauses;
}