package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CustomerLeadCreateDTO {

    private String leadName;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String companyName;

    private Integer industryType;

    private Integer leadSource;

    private Integer leadLevel;

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
}