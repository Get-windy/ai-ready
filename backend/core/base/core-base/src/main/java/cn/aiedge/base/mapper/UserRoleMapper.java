package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 删除用户的所有角色关联
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除角色的所有用户关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入用户角色关联
     */
    int batchInsert(@Param("list") List<UserRole> list);

    /**
     * 查询用户的角色ID列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
