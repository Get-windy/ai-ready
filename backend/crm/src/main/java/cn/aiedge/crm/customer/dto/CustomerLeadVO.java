package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerLeadVO {

    private Long id;

    private String leadCode;

    private String leadName;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String companyName;

    private Integer industryType;

    private String industryTypeDesc;

    private Integer leadSource;

    private String leadSourceDesc;

    private Integer leadStatus;

    private String leadStatusDesc;

    private Integer leadLevel;

    private String leadLevelDesc;

    private BigDecimal estimatedAmount;

    private String province;

    private String city;

    private String address;

    private String requirement;

    private String remark;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private LocalDate expectedCloseDate;

    private LocalDate actualCloseDate;

    private Long convertedCustomerId;

    private LocalDateTime convertedTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private Integer version;
}