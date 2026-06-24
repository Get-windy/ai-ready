package cn.aiedge.hr;

import cn.aiedge.hr.organization.HrPosition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface HrPositionMapper extends BaseMapper<HrPosition> {
    List<HrPosition> selectByDeptId(@Param("deptId") Long deptId);
}
