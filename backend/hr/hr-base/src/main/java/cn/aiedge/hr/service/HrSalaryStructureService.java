package cn.aiedge.hr.service;

import cn.aiedge.hr.salary.HrSalaryStructure;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 员工薪资结构服务
 */
public interface HrSalaryStructureService extends IService<HrSalaryStructure> {

    Page<HrSalaryStructure> pageStructures(Page<HrSalaryStructure> page, Long tenantId,
                                            Long employeeId, Integer status);

    /** 取员工当前有效薪资结构（status=1 且生效日期最近的一条） */
    HrSalaryStructure getEffectiveByEmployeeId(Long employeeId);

    List<HrSalaryStructure> listByEmployee(Long employeeId);

    Long createStructure(HrSalaryStructure structure);

    void updateStructure(HrSalaryStructure structure);

    void deleteStructure(Long id);
}
