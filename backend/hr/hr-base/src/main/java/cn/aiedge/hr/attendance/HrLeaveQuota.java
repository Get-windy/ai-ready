package cn.aiedge.hr.attendance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 假期额度（按租户 × 请假类型 × 年度）
 *
 * <p>对标 Odoo 的假期三件套中的 **额度分配（Allocation）**：本表是「某一类假期在某一年的可用天数」，
 * 请假申请批准后据本表校验余额并累计已用天数（已用为实时聚集，不落冗余列，避免双写不一致）。</p>
 *
 * <p><b>本系统尚未实现的部分</b>（如实登记，勿当成已有能力）：</p>
 * <ul>
 *   <li>**按工时/按月累积（Accrual）** —— 无 `accrual_plan`，额度需人工维护；</li>
 *   <li>**结转规则（Rollover）** —— 无「不结转 / 上限结转 / 封顶」配置，跨年额度不自动结转；</li>
 *   <li>**有效期与扣减期分离**（SAP IT2006）—— 只有「年度」一个维度。</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_leave_quota")
public class HrLeaveQuota {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 请假类型：ANNUAL / SICK / PERSONAL / MATERNITY / MARRIAGE */
    private String leaveType;

    /** 年度 */
    private Integer year;

    /** 年度额度（天） */
    private BigDecimal quotaDays;

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
