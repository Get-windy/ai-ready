package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerFollowUpVO {

    private Long id;

    private String followUpCode;

    private Long customerId;

    private String customerName;

    private Long opportunityId;

    private String opportunityName;

    private Long leadId;

    private String leadName;

    private Integer followUpType;

    private String followUpTypeDesc;

    private String contactName;

    private String contactPhone;

    private LocalDate followUpDate;

    private String content;

    private String nextAction;

    private LocalDate nextFollowUpDate;

    private Integer followUpResult;

    private String followUpResultDesc;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private Integer version;
}