package cn.aiedge.erp.party.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MD客户视图对象——向前端映射旧版 partner 字段名
 */
@Data
public class MdCustomerVO {
    private Long id;
    private String partnerCode;
    private String partnerName;
    private String partnerShortName;
    private String partnerType;
    private Long categoryId;
    private String categoryName;
    private String gradeName;
    private String settleType;
    private String phone;
    private String fax;
    private String email;
    private String website;
    private String legalPerson;
    private String taxNumber;
    private String bankName;
    private String bankAccount;
    private BigDecimal creditLimit;
    private String remark;
    private String status;
    private String statusDesc;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
