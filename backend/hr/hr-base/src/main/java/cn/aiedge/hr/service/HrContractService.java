package cn.aiedge.hr.service;

import cn.aiedge.hr.employee.HrContract;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface HrContractService extends IService<HrContract> {
    List<HrContract> getByEmployeeId(Long employeeId);
    Long createContract(HrContract contract);
}
