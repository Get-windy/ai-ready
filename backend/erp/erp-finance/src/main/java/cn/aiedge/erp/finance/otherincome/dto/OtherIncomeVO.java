package cn.aiedge.erp.finance.otherincome.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 其他收入单列表/详情 VO（按单据口径）
 */
@Data
public class OtherIncomeVO {

    private Long id;

    private String docNo;

    /** 收入类型: 1-往来单位收入 2-内部收入 */
    private Integer incomeType;

    private String incomeTypeName;

    private BigDecimal amount;

    private String currency;

    private Long partnerId;

    private String partnerName;

    private String partnerCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate incomeDate;

    private Long handlerId;

    private String handlerName;

    private Long departmentId;

    private String departmentName;

    private String receiptAccount1;

    private BigDecimal receiptAmount1;

    private String receiptAccount2;

    private BigDecimal receiptAmount2;

    private String receiptAccount3;

    private BigDecimal receiptAmount3;

    private String receiptAccount4;

    private BigDecimal receiptAmount4;

    private String accountSubjectCode;

    private String summary;

    private String attachment;

    private Integer status;

    private String statusDesc;

    private Integer settleStatus;

    private String source;

    private String settlementMethod;

    private String bankAccount;

    private String bankName;

    private String transactionNo;

    private Long creatorId;

    private String creatorName;

    private Long bookkeeperId;

    private String bookkeeperName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bookkeepingTime;

    private Integer printCount;

    private String voucherNo;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Integer versionNo;

    private List<?> items;
}
