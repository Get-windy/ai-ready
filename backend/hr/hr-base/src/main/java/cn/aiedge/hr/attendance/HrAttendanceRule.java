package cn.aiedge.hr.attendance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考勤规则（班次 / 上下班时间 / 迟到早退阈值）
 *
 * <p>此前本系统**完全没有考勤规则**——打卡只写 `status = NORMAL`，`late_minutes` /
 * `early_minutes` / `work_hours` 三列永远为空，于是「迟到早退永远判不出来、加班时长永远为 0」，
 * 薪资侧的加班/缺勤计算也就没有可信输入。</p>
 *
 * <p>本表按租户一行（`tenant_id` 唯一），由「考勤记录」页的考勤规则弹窗维护；
 * 打卡与重算均以它为准。默认值：09:00 上班、18:00 下班、宽限 0 分钟、标准工时 8 小时。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("hr_attendance_rule")
public class HrAttendanceRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 上班时间 */
    private LocalTime workStartTime;

    /** 下班时间 */
    private LocalTime workEndTime;

    /** 迟到宽限分钟数（打卡晚于 上班时间+宽限 才判迟到） */
    private Integer lateGraceMinutes;

    /** 早退宽限分钟数 */
    private Integer earlyGraceMinutes;

    /** 标准日工时（小时） */
    private java.math.BigDecimal standardWorkHours;

    /** 是否启用「未打卡即缺勤」自动判定 */
    private Integer autoAbsent;

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
