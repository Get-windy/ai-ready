package cn.aiedge.hr;

import cn.aiedge.hr.performance.HrPerformance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface HrPerformanceMapper extends BaseMapper<HrPerformance> {
    List<HrPerformance> selectByEmployeeId(@Param("employeeId") Long employeeId);
}
