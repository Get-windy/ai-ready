package cn.aiedge.erp.party.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PartyDTO {

    private Long id;

    private String partyCode;

    private String partyName;

    private String shortName;

    private Integer partyType;

    private String partyTypeDesc;

    private Long categoryId;

    private String categoryName;

    private String partyLevel;

    private BigDecimal creditLimit;

    private BigDecimal currentDebt;

    private Integer settlementType;

    private String settlementTypeDesc;

    private Integer settlementDays;

    private String unifiedCode;

    private String businessLicense;

    private String taxNumber;

    private String bankName;

    private String bankAccount;

    private String phone;

    private String fax;

    private String email;

    private String website;

    private String legalPerson;

    private String legalPersonPhone;

    private Integer status;

    private String statusDesc;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<PartyContactDTO> contacts;
}
