package cn.aiedge.hr.mapper;

import cn.aiedge.hr.employee.HrEmployee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工 Mapper
 *
 * <p>本接口**不声明任何自定义方法**：此前 3 个自定义方法（selectByDeptId / selectByEmployeeNo /
 * updateStatus）既无 XML 也无 &#64;Select，一旦调用即抛 {@code Invalid bound statement (not found)}
 * （转正/离职必挂）。现已全部改为 Service 层用 LambdaQueryWrapper / UpdateWrapper 实现——
 * 这样多租户拦截器与逻辑删除才能正常生效。</p>
 */
@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {
}
