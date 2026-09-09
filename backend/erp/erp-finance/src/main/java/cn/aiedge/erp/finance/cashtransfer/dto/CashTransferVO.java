package cn.aiedge.erp.finance.cashtransfer.dto;

import cn.aiedge.erp.finance.cashtransfer.entity.CashTransferItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提存详情 VO（含转入账户明细 items）
 */
@Data
public class CashTransferVO {

    private Long id;

    private Long tenantId;

    private String docNo;

    private LocalDate docDate;

    private Long fromAccountId;

    private String fromAccountName;

    private Integer fromAccountType;

    private String fromSubjectCode;

    private BigDecimal fromAmount;

    private BigDecimal fee;

    private BigDecimal toAmount;

    private BigDecimal totalAmount;

    private Long handlerId;

    private String handlerName;

    private Long deptId;

    private String deptName;

    private Integer status;

    private String creatorName;

    private Long bookkeeperId;

    private String bookkeeperName;

    private LocalDateTime bookkeepingTime;

    private String summary;

    private String attachment;

    private String remark;

    private Integer printCount;

    private Integer redFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 转入账户明细 */
    private List<CashTransferItem> items;
}
