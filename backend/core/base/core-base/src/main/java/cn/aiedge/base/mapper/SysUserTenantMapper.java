package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUserTenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-租户关联 Mapper
 *
 * @author AI-Ready Team
 * @since 1.1.8
 */
@Mapper
public interface SysUserTenantMapper extends BaseMapper<SysUserTenant> {

    /**
     * 查询用户可访问的租户列表
     */
    @Select("SELECT t.* FROM sys_tenant t " +
            "INNER JOIN sys_user_tenant ut ON t.id = ut.tenant_id " +
            "WHERE ut.user_id = #{userId} " +
            "  AND ut.status = 1 " +
            "  AND t.status = 1 " +
            "  AND t.deleted = 0 " +
            "ORDER BY ut.is_default DESC, t.tenant_name ASC")
    List<SysTenant> selectTenantsByUserId(@Param("userId") Long userId);

    /**
     * 查询用户在指定租户的关联记录
     */
    @Select("SELECT * FROM sys_user_tenant " +
            "WHERE user_id = #{userId} AND tenant_id = #{tenantId} AND status = 1 " +
            "LIMIT 1")
    SysUserTenant selectByUserAndTenant(@Param("userId") Long userId,
                                        @Param("tenantId") Long tenantId);
}
