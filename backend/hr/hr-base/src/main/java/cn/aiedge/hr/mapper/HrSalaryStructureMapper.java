package cn.aiedge.hr.mapper;

import cn.aiedge.hr.salary.HrSalaryStructure;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 薪资结构 Mapper
 *
 * <p>本接口**不声明任何自定义方法**（原 selectEffectiveByEmployeeId 无 SQL 绑定，
 * 薪资结构查询与月度薪资生成必抛 {@code Invalid bound statement}），
 * 改由 Service 层 LambdaQueryWrapper 实现。</p>
 */
@Mapper
public interface HrSalaryStructureMapper extends BaseMapper<HrSalaryStructure> {
}
