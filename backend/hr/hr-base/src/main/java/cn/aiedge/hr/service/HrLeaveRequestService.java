package cn.aiedge.hr.service;

import cn.aiedge.hr.attendance.HrLeaveRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface HrLeaveRequestService extends IService<HrLeaveRequest> {
    Page<HrLeaveRequest> pageRequests(Page<HrLeaveRequest> page, Long tenantId, Long employeeId, Integer status);
    Long submitRequest(HrLeaveRequest request);
    void approve(Long id, Long approveId, String comment);
    void reject(Long id, Long approveId, String comment);
}
