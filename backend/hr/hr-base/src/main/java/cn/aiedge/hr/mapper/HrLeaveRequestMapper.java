package cn.aiedge.hr.mapper;

import cn.aiedge.hr.attendance.HrLeaveRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请假申请 Mapper
 *
 * <p>本接口**不声明任何自定义方法**（原 selectByEmployeeId 无 SQL 绑定），
 * 改由 Service 层 LambdaQueryWrapper 实现。</p>
 */
@Mapper
public interface HrLeaveRequestMapper extends BaseMapper<HrLeaveRequest> {
}
