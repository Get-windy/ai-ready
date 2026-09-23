package cn.aiedge.hr.service;

import cn.aiedge.hr.attendance.HrLeaveQuota;
import cn.aiedge.hr.attendance.HrLeaveRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 请假申请服务
 */
public interface HrLeaveRequestService extends IService<HrLeaveRequest> {

    Page<HrLeaveRequest> pageRequests(Page<HrLeaveRequest> page, Long tenantId,
                                      Long employeeId, Integer status,
                                      String leaveType, Long deptId,
                                      LocalDate startDateFrom, LocalDate startDateTo);

    Long submitRequest(HrLeaveRequest request);

    /** 批准：置状态 1 + 写考勤「休假」标记（与考勤联动） */
    void approve(Long id, Long approveId, String comment);

    void reject(Long id, Long approveId, String comment);

    /** 撤销：仅待审批可撤销 */
    void cancel(Long id);

    void updateRequest(HrLeaveRequest request);

    void deleteRequest(Long id);

    List<HrLeaveRequest> listByEmployee(Long employeeId);

    /**
     * 假期额度余额：按租户年度额度（`hr_leave_quota`）减去该员工本年度**已批准**天数。
     * 未配置额度的类型按 0 处理，且不拦截申请（避免"未配置即无法请假"）。
     */
    Map<String, Object> balance(Long employeeId, Integer year);

    /** 年度额度清单（全部请假类型） */
    List<HrLeaveQuota> listQuotas(Integer year);

    /** 保存年度额度（按 类型 + 年度 upsert） */
    HrLeaveQuota saveQuota(HrLeaveQuota quota);

    /** 请假统计：待审批/已批准/已拒绝 单数、批准总天数 */
    Map<String, Object> statistics(Integer year, Long deptId);
}
