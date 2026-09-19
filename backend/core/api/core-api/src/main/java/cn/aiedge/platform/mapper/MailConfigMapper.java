package cn.aiedge.platform.mapper;

import cn.aiedge.platform.model.MailConfig;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MailConfigMapper extends BaseMapper<MailConfig> {

    /**
     * 取当前生效的邮件配置（平台级全局优先）。
     *
     * <p>两条口径说明：
     * <ol>
     *   <li>{@code @InterceptorIgnore(tenantLine = "true")} —— 本表是**平台级配置**，
     *       `tenant_id` 是「这条配置属于哪个租户」的**归属标记**，不是会话过滤条件。
     *       不加这个注解时，发信链路若恰好带着某个会话租户，会被注入
     *       `AND tenant_id = <会话租户>` → 平台级行（`tenant_id=0`）读不到。</li>
     *   <li>`ORDER BY (tenant_id = 0) DESC` —— PostgreSQL 布尔降序即「true 在前」，
     *       因此**平台级全局行（0）永远优先**，其次才是任意租户行；
     *       同一优先级内按 `id ASC` 取最早的一条，保证结果稳定（不会每次随机取一行）。</li>
     * </ol>
     * 本表**没有** `deleted` 列（实测 11 列），故不加逻辑删条件。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_mail_config ORDER BY (tenant_id = 0) DESC, id ASC LIMIT 1")
    MailConfig selectEffective();
}
