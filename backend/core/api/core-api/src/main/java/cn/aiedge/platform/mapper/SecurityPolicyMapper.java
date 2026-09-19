package cn.aiedge.platform.mapper;

import cn.aiedge.platform.model.SecurityPolicy;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SecurityPolicyMapper extends BaseMapper<SecurityPolicy> {

    /**
     * 取当前生效的安全策略（平台级全局优先）。
     *
     * <p>口径与 {@link MailConfigMapper#selectEffective()} 一致：
     * {@code @InterceptorIgnore} 因为本表是平台级配置、`tenant_id` 是归属标记而非会话过滤条件；
     * `ORDER BY (tenant_id = 0) DESC` 让平台级行优先，同级内按 id 取最早的一条。
     *
     * <p><b>只取 `enabled = true` 的行</b>：页面上的「启用」开关若关掉，
     * 期望语义是"回到系统默认策略"，而不是"继续用这份策略"。
     * 本表**没有** `deleted` 列（实测 24 列）。
     *
     * <p>调用方：{@code DbPlatformSettingsProvider}（供 `PasswordPolicy` 与密码有效期判定使用）。
     * 注意本查询**每次调用都查库**，不做缓存 —— 这样"页面改完策略下一次改密码即生效"，
     * 且省掉缓存失效的一整套复杂度（单行主键查询，代价可忽略）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_security_policy WHERE enabled = true "
            + "ORDER BY (tenant_id = 0) DESC, id ASC LIMIT 1")
    SecurityPolicy selectEffective();
}
