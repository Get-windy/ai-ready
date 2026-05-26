package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色编码查询角色
     */
    Role selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 根据用户ID查询角色列表
     */
    List<Role> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询子角色列表
     */
    List<Role> selectByParentId(@Param("parentId") Long parentId);
}
