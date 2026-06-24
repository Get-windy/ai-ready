package cn.aiedge.hr.service;

import cn.aiedge.hr.attendance.HrAttendance;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface HrAttendanceService extends IService<HrAttendance> {
    Page<HrAttendance> pageAttendances(Page<HrAttendance> page, Long tenantId, Long employeeId, String month);
    List<HrAttendance> getByEmployeeAndMonth(Long employeeId, String month);
    void clockIn(Long employeeId);
    void clockOut(Long employeeId);
}
