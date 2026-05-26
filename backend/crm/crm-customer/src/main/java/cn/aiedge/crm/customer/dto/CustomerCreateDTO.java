package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CustomerCreateDTO {

    private String customerName;

    private String shortName;

    private Integer customerType;

    private Integer customerSource;

    private Integer industryType;

    private String province;

    private String city;

    private String district;

    private String address;

    private String phone;

    private String fax;

    private String email;

    private String website;

    private String legalPerson;

    private String businessContact;

    private String businessContactPhone;

    private String financeContact;

    private String financeContactPhone;

    private String taxNumber;

    private String bankName;

    private String bankAccount;

    private Integer customerLevel;

    private BigDecimal creditLimit;

    private Integer settlementType;

    private Integer settlementDays;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String remark;
}