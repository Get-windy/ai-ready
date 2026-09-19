package cn.aiedge.hr.attendance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请实体
 * 员工请假申请记录
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_leave_request")
public class HrLeaveRequest {

    /**
     * 请假ID
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
     * 请假类型（ANNUAL-年假 SICK-病假 PERSONAL-事假 MATERNITY-产假 MARRIAGE-婚假）
     */
    private String leaveType;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 请假天数
     */
    private BigDecimal days;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 申请状态（0-待审批 1-已批准 2-已拒绝 3-已撤销）
     */
    private Integer status;

    /**
     * 审批人ID
     */
    private Long approveId;

    /**
     * 审批意见
     */
    private String approveComment;

    /**
     * 审批时间
     */
    private LocalDateTime approveTime;

    /**
     * 工作流实例ID
     */
    private Long workflowInstanceId;

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

    // ── 展示用联表字段（非表列） ──

    /** 员工姓名 */
    @TableField(exist = false)
    private String employeeName;

    /** 员工工号 */
    @TableField(exist = false)
    private String employeeNo;

    /** 部门名称 */
    @TableField(exist = false)
    private String deptName;

    /** 审批人姓名 */
    @TableField(exist = false)
    private String approveName;
}
