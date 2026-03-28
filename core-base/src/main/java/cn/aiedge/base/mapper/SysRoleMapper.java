package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据角色编码查询
     */
    SysRole selectByRoleCode(@Param("roleCode") String roleCode, @Param("tenantId") Long tenantId);

    /**
     * 查询用户的角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 分页查询角色列表
     */
    Page<SysRole> selectRolePage(Page<SysRole> page,
                                  @Param("tenantId") Long tenantId,
                                  @Param("roleName") String roleName,
                                  @Param("status") Integer status);

    /**
     * 检查角色编码是否存在
     */
    int checkRoleCodeExists(@Param("roleCode") String roleCode, 
                            @Param("tenantId") Long tenantId,
                            @Param("excludeId") Long excludeId);
}