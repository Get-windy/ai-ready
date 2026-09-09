package cn.aiedge.erp.finance.cashtransfer.dto;

import cn.aiedge.erp.finance.cashtransfer.entity.CashTransferItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 提存保存请求(DTO)
 * 保存草稿/更新时传输头字段 + 转入账户明细。
 */
@Data
public class CashTransferSaveDTO {

    private Long id;

    private String docNo;

    private LocalDate docDate;

    /** 转出账户ID */
    private Long fromAccountId;

    private String fromAccountName;

    private Integer fromAccountType;

    private String fromSubjectCode;

    /** 转出金额 = Σ转入金额 + 手续费 */
    private BigDecimal fromAmount;

    /** 手续费 */
    private BigDecimal fee;

    /** 经手人 */
    private Long handlerId;

    private String handlerName;

    private Long deptId;

    private String deptName;

    /** 制单人 */
    private String creatorName;

    private String summary;

    private String remark;

    /** 转入账户明细 */
    private List<CashTransferItem> items;
}
