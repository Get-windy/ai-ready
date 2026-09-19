package cn.aiedge.hr.mapper;

import cn.aiedge.hr.employee.HrContract;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 劳动合同 Mapper
 *
 * <p>本接口**不声明任何自定义方法**（原 selectByEmployeeId 无 SQL 绑定，调用即抛
 * {@code Invalid bound statement}），改由 Service 层 LambdaQueryWrapper 实现。</p>
 */
@Mapper
public interface HrContractMapper extends BaseMapper<HrContract> {
}
