package cn.aiedge.hr.service;

import cn.aiedge.hr.salary.HrSalaryStructure;
import com.baomidou.mybatisplus.extension.service.IService;

public interface HrSalaryStructureService extends IService<HrSalaryStructure> {
    HrSalaryStructure getEffectiveByEmployeeId(Long employeeId);
    Long createStructure(HrSalaryStructure structure);
}
