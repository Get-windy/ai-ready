package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-角色关联Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 批量插入用户角色关联
     */
    int batchInsert(@Param("list") List<SysUserRole> list);

    /**
     * 根据用户ID删除角色关联
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID删除用户关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询用户的角色ID列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询角色下的用户ID列表
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
