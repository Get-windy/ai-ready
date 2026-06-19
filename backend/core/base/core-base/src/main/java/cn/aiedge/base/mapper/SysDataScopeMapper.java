package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysDataScope;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据权限范围 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysDataScopeMapper extends BaseMapper<SysDataScope> {

    /**
     * 根据角色ID列表查询数据权限规则
     */
    List<SysDataScope> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据角色ID和目标表名查询数据权限规则
     */
    List<SysDataScope> selectByRoleAndTable(@Param("roleId") Long roleId, @Param("tableName") String tableName);
}
