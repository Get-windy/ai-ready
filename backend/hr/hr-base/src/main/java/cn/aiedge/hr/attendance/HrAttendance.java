package cn.aiedge.hr.attendance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考勤记录实体
 * 员工每日打卡记录
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_attendance")
public class HrAttendance {

    /**
     * 考勤ID
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
     * 考勤日期
     */
    private LocalDate attendanceDate;

    /**
     * 上班打卡时间
     */
    private LocalTime clockInTime;

    /**
     * 下班打卡时间
     */
    private LocalTime clockOutTime;

    /**
     * 考勤状态（NORMAL-正常 LATE-迟到 EARLY-早退 ABSENT-缺勤）
     */
    private String status;

    /**
     * 迟到分钟数
     */
    private Integer lateMinutes;

    /**
     * 早退分钟数
     */
    private Integer earlyMinutes;

    /**
     * 工作时长（小时）
     */
    private BigDecimal workHours;

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