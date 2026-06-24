package cn.aiedge.hr;

import cn.aiedge.hr.employee.HrEmployee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {
    List<HrEmployee> selectByDeptId(@Param("deptId") Long deptId);
    HrEmployee selectByEmployeeNo(@Param("employeeNo") String employeeNo);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
