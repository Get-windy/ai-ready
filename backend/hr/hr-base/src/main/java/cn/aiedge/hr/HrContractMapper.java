package cn.aiedge.hr;

import cn.aiedge.hr.employee.HrContract;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface HrContractMapper extends BaseMapper<HrContract> {
    List<HrContract> selectByEmployeeId(@Param("employeeId") Long employeeId);
}
