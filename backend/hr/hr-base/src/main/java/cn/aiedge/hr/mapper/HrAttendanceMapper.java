package cn.aiedge.hr.mapper;

import cn.aiedge.hr.attendance.HrAttendance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤记录 Mapper
 *
 * <p>本接口**不声明任何自定义方法**（原 selectByEmployeeAndMonth 无 SQL 绑定，月度薪资生成必抛
 * {@code Invalid bound statement}），改由 Service 层 LambdaQueryWrapper 实现。</p>
 */
@Mapper
public interface HrAttendanceMapper extends BaseMapper<HrAttendance> {
}
