package cn.aiedge.crm.customer.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerVO {

    private Long id;

    private String customerCode;

    private String customerName;

    private String shortName;

    private Integer customerType;

    private String customerTypeDesc;

    private Integer customerSource;

    private String customerSourceDesc;

    private Integer industryType;

    private String industryTypeDesc;

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

    private String customerLevelDesc;

    private BigDecimal creditLimit;

    private BigDecimal currentDebt;

    private Integer settlementType;

    private String settlementTypeDesc;

    private Integer settlementDays;

    private Integer status;

    private String statusDesc;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private LocalDate firstTradeDate;

    private LocalDate lastTradeDate;

    private Integer tradeCount;

    private BigDecimal tradeAmount;

    private BigDecimal potentialAmount;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;

    private Integer version;
}