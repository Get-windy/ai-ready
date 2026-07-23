package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计期间实体
 * status: 1-开启 0-关闭（已月结）
 */
@Data
@TableName("fin_accounting_period")
@EqualsAndHashCode(callSuper = true)
public class AccountingPeriod extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 会计年度
     */
    @TableField("period_year")
    private Integer periodYear;

    /**
     * 会计月份 (1-12)
     */
    @TableField("period_month")
    private Integer periodMonth;

    /**
     * 期间编码，格式 yyyy-MM，如 2026-07
     */
    @TableField("period_code")
    private String periodCode;

    /**
     * 期间开始日期
     */
    @TableField("start_date")
    private LocalDate startDate;

    /**
     * 期间结束日期
     */
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 状态：1-开启 0-关闭（已月结）
     */
    @TableField("status")
    private Integer status = 1;

    /**
     * 月结操作人
     */
    @TableField("closed_by")
    private String closedBy;

    /**
     * 月结时间
     */
    @TableField("closed_time")
    private LocalDateTime closedTime;
}
