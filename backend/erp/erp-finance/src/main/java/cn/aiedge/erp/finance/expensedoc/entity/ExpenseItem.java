package cn.aiedge.erp.finance.expensedoc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用项明细实体
 * 一单多费用项：费用编号/费用名称/费用科目/金额/备注。
 * 费用项明细映射费用类会计科目（凭证借方）。
 */
@Data
@Accessors(chain = true)
@TableName("erp_expense_item")
public class ExpenseItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 主表ID */
    private Long expenseDocId;

    /** 行号 */
    private Integer lineNo;

    /** 费用编号 */
    private String expenseCode;

    /** 费用名称 */
    private String expenseName;

    /** 费用科目编码(凭证用) */
    private String subjectCode;

    /** 费用科目名称 */
    private String subjectName;

    /** 金额 */
    private BigDecimal amount;

    /** 明细备注 */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
