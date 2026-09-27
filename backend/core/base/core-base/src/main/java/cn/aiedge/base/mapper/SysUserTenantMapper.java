package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysUserTenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
     * <p>
     * 排序即「登录时选企业的候选顺序」：上次登录的排第一（PG 中 DESC 默认 NULLS FIRST，
     * 故必须显式写 NULLS LAST，否则没登录过的企业会窜到最前面）。
     * </p>
     * <p>
     * {@code t.id > 0} 排除 0 号「全局默认/模板容器」记录 —— 它不是真租户，
     * 不得作为登录或切换目标（见 AI_DEVELOPER_RULES §9.1）。
     * </p>
     */
    @Select("SELECT t.* FROM sys_tenant t " +
            "INNER JOIN sys_user_tenant ut ON t.id = ut.tenant_id " +
            "WHERE ut.user_id = #{userId} " +
            "  AND ut.status = 1 " +
            "  AND t.status = 1 " +
            "  AND t.deleted = 0 " +
            "  AND t.id > 0 " +
            "ORDER BY ut.last_login_time DESC NULLS LAST, ut.is_default DESC, t.tenant_name ASC")
    List<SysTenant> selectTenantsByUserId(@Param("userId") Long userId);

    /**
     * 记录「上次登录该租户的时间」
     */
    @Update("UPDATE sys_user_tenant SET last_login_time = now() " +
            "WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    int updateLastLoginTime(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 查询该用户上次登录的租户ID；从未登录过返回 null
     * <p>登录时用于在候选企业里标出「上次登录」，与 selectTenantsByUserId 的排序口径一致。</p>
     */
    @Select("SELECT tenant_id FROM sys_user_tenant " +
            "WHERE user_id = #{userId} AND last_login_time IS NOT NULL " +
            "ORDER BY last_login_time DESC LIMIT 1")
    Long selectLastLoginTenantId(@Param("userId") Long userId);

    /**
     * 查询用户在指定租户的关联记录
     */
    @Select("SELECT * FROM sys_user_tenant " +
            "WHERE user_id = #{userId} AND tenant_id = #{tenantId} AND status = 1 " +
            "LIMIT 1")
    SysUserTenant selectByUserAndTenant(@Param("userId") Long userId,
                                        @Param("tenantId") Long tenantId);
}
