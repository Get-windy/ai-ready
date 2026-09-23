package cn.aiedge.hr.service;

import cn.aiedge.hr.attendance.HrAttendance;
import cn.aiedge.hr.attendance.HrAttendanceRule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤记录服务
 */
public interface HrAttendanceService extends IService<HrAttendance> {

    Page<HrAttendance> pageAttendances(Page<HrAttendance> page, Long tenantId,
                                       Long employeeId, String month,
                                       String status, Long deptId,
                                       LocalDate dateStart, LocalDate dateEnd);

    List<HrAttendance> getByEmployeeAndMonth(Long employeeId, String month);

    void clockIn(Long employeeId);

    void clockOut(Long employeeId);

    Long createAttendance(HrAttendance attendance);

    void updateAttendance(HrAttendance attendance);

    void deleteAttendance(Long id);

    void batchDelete(List<Long> ids);

    /** 考勤统计：正常/迟到/早退/缺勤/休假 天数、总工时 */
    Map<String, Object> statistics(String month, Long deptId, Long employeeId);

    /** 重算迟到/早退/工时（按当前租户的考勤规则） */
    void recalculate(Long id);

    /** 读取当前租户的考勤规则（不存在时返回默认值，不落库） */
    HrAttendanceRule getRule();

    /** 保存考勤规则（租户内单行，不存在则新建） */
    HrAttendanceRule saveRule(HrAttendanceRule rule);

    /** 把一段日期标记为「休假」（请假批准时调用，考勤↔请假联动） */
    java.util.List<HrAttendance> markLeave(Long employeeId, java.time.LocalDate start,
                                           java.time.LocalDate end, Long leaveRequestId);

    /** 还原某请假单写入的「休假」标记（请假撤销/拒绝时调用） */
    void unmarkLeave(Long leaveRequestId);
}
