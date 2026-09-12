package cn.aiedge.erp.finance.expensedoc.dto;

import cn.aiedge.erp.finance.expensedoc.entity.ExpenseItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用单保存请求(DTO)
 * 保存草稿/更新时传输头字段 + 费用项明细。
 */
@Data
public class ExpenseDocSaveDTO {

    private Long id;

    private String docNo;

    private LocalDate docDate;

    /** 费用类型 0-往来单位费用 1-内部费用 */
    private Integer expenseType;

    /** 往来单位（往来单位费用） */
    private Long partnerId;

    private String partnerCode;

    private String partnerName;

    /** 经手人 */
    private Long handlerId;

    private String handlerName;

    /** 部门（内部费用） */
    private Long deptId;

    private String deptName;

    /** 付款账户1 */
    private Long payAccountId;

    private String payAccountName;

    private BigDecimal payAmount;

    /** 付款账户2 */
    private Long payAccount2Id;

    private String payAccount2Name;

    private BigDecimal payAmount2;

    /** 付款账户3 */
    private Long payAccount3Id;

    private String payAccount3Name;

    private BigDecimal payAmount3;

    /** 付款账户4 */
    private Long payAccount4Id;

    private String payAccount4Name;

    private BigDecimal payAmount4;

    /** 制单人 */
    private String creatorName;

    private String summary;

    private String remark;

    /** 费用项明细 */
    private List<ExpenseItem> items;
}
