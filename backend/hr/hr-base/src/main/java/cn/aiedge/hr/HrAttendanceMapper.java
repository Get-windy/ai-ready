package cn.aiedge.hr;

import cn.aiedge.hr.attendance.HrAttendance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface HrAttendanceMapper extends BaseMapper<HrAttendance> {
    List<HrAttendance> selectByEmployeeAndMonth(@Param("employeeId") Long employeeId, @Param("month") String month);
}
