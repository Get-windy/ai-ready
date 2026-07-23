package cn.aiedge.hr.mapper;

import cn.aiedge.hr.attendance.HrLeaveRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface HrLeaveRequestMapper extends BaseMapper<HrLeaveRequest> {
    List<HrLeaveRequest> selectByEmployeeId(@Param("employeeId") Long employeeId);
}
