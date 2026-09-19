package cn.aiedge.hr.mapper;

import cn.aiedge.hr.ref.HrDepartmentRef;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门只读引用 Mapper（system 管理域的 sys_department，HR 侧只读）
 */
@Mapper
public interface HrDepartmentRefMapper extends BaseMapper<HrDepartmentRef> {
}
