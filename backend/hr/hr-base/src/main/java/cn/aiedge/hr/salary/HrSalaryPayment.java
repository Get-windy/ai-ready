package cn.aiedge.hr.salary;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 薪资发放实体
 * 员工月度工资发放记录
 *
 * @author AI-Ready Team
 * * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_salary_payment")
public class HrSalaryPayment {

    /**
     * 发放ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 发放月份（格式：yyyy-MM）
     */
    private String paymentMonth;

    /**
     * 基本工资
     */
    private BigDecimal baseAmount;

    /**
     * 绩效工资
     */
    private BigDecimal performanceAmount;

    /**
     * 津贴补贴合计
     */
    private BigDecimal allowanceAmount;

    /**
     * 加班工资
     */
    private BigDecimal overtimeAmount;

    /**
     * 扣款合计（迟到、请假等）
     */
    private BigDecimal deductAmount;

    /**
     * 社保扣款
     */
    private BigDecimal socialDeduct;

    /**
     * 公积金扣款
     */
    private BigDecimal fundDeduct;

    /**
     * 个税扣款
     */
    private BigDecimal taxDeduct;

    /**
     * 实发金额
     */
    private BigDecimal actualAmount;

    /**
     * 发放日期
     */
    private LocalDate paymentDate;

    /**
     * 发放状态（0-待发放 1-已发放 2-已撤销）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}