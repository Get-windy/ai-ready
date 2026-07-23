package cn.aiedge.hr.mapper;

import cn.aiedge.hr.salary.HrSalaryStructure;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HrSalaryStructureMapper extends BaseMapper<HrSalaryStructure> {
    HrSalaryStructure selectEffectiveByEmployeeId(@Param("employeeId") Long employeeId);
}
