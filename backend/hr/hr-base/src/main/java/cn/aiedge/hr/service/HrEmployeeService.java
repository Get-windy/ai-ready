package cn.aiedge.hr.service;

import cn.aiedge.hr.employee.HrEmployee;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface HrEmployeeService extends IService<HrEmployee> {
    Page<HrEmployee> pageEmployees(Page<HrEmployee> page, Long tenantId, Long deptId, String employeeName, Integer status);
    HrEmployee getByEmployeeNo(String employeeNo);
    Long createEmployee(HrEmployee employee);
    void updateEmployee(HrEmployee employee);
    void updateStatus(Long id, Integer status);
    List<HrEmployee> getByDeptId(Long deptId);
}
