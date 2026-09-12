package cn.aiedge.erp.finance.otherincome.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 其他收入单-收入项明细实体（金标准）
 * 收入项映射收益类科目。
 */
@Data
@Accessors(chain = true)
@TableName("fin_other_income_doc_item")
public class OtherIncomeDocItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 其他收入单ID */
    private Long docId;

    /** 行序号 */
    private Integer lineNo;

    /** 收入编号 */
    private String incomeNo;

    /** 收入名称 */
    private String incomeName;

    /** 金额 */
    private BigDecimal amount;

    /** 收益类科目编码 */
    private String subjectCode;

    /** 备注 */
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
}
