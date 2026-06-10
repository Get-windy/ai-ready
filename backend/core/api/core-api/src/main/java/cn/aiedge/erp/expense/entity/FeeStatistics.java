package cn.aiedge.erp.expense.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用统计台账表
 */
@Data
@TableName("fee_statistics")
public class FeeStatistics {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 统计年份 */
    private Integer statYear;

    /** 统计月份 */
    private Integer statMonth;

    /** 统计日期 */
    private LocalDate statDate;

    /** 部门ID(NULL=全部) */
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 费用类型(NULL=全部) */
    private String expenseType;

    /** 申请数量 */
    private Integer applyCount;

    /** 申请金额 */
    private BigDecimal applyAmount;

    /** 已审批数量 */
    private Integer approvedCount;

    /** 已审批金额 */
    private BigDecimal approvedAmount;

    /** 已拒绝数量 */
    private Integer rejectedCount;

    /** 已拒绝金额 */
    private BigDecimal rejectedAmount;

    /** 报销数量 */
    private Integer reimbursementCount;

    /** 报销金额 */
    private BigDecimal reimbursementAmount;

    /** 已付款数量 */
    private Integer paidCount;

    /** 已付款金额 */
    private BigDecimal paidAmount;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 预算使用率(%) */
    private BigDecimal budgetUsageRate;

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
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
