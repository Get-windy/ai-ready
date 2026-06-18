package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerFollowUpCreateDTO {

    private Long customerId;

    private Long opportunityId;

    private Long leadId;

    private Integer followUpType;

    private String contactName;

    private String contactPhone;

    private LocalDate followUpDate;

    private String content;

    private String nextAction;

    private LocalDate nextFollowUpDate;

    private Integer followUpResult;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String remark;
}