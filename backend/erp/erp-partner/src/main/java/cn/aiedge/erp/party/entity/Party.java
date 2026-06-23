package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party")
public class Party {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String partyCode;

    private String partyName;

    private String shortName;

    private Integer partyType;

    private Long categoryId;

    private String partyLevel;

    private BigDecimal creditLimit;

    private BigDecimal currentDebt;

    private Integer settlementType;

    private Integer settlementDays;

    private String unifiedCode;

    private String businessLicense;

    private String taxNumber;

    private String bankName;

    private String bankAccount;

    private String registeredAddress;

    private String businessAddress;

    private String phone;

    private String fax;

    private String email;

    private String website;

    private String legalPerson;

    private String legalPersonPhone;

    private String businessContact;

    private String businessContactPhone;

    private String financeContact;

    private String financeContactPhone;

    private LocalDate firstTradeDate;

    private LocalDate lastTradeDate;

    private Integer tradeCount;

    private BigDecimal tradeAmount;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
