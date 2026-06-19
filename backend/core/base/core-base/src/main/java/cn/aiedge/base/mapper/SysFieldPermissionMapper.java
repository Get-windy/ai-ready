package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysFieldPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字段级权限 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysFieldPermissionMapper extends BaseMapper<SysFieldPermission> {

    /**
     * 根据角色ID列表查询字段权限
     */
    List<SysFieldPermission> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据角色ID和目标表名查询字段权限
     */
    List<SysFieldPermission> selectByRoleAndTable(@Param("roleIds") List<Long> roleIds, @Param("tableName") String tableName);
}
