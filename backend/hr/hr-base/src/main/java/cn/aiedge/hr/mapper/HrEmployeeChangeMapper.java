package cn.aiedge.hr.mapper;

import cn.aiedge.hr.change.HrEmployeeChange;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人事异动记录 Mapper
 */
@Mapper
public interface HrEmployeeChangeMapper extends BaseMapper<HrEmployeeChange> {
}
