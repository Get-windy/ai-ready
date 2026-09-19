package cn.aiedge.erp.marketing.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 营销域「有活的租户」清单（定时作业逐租户执行的入口）。
 *
 * <p>定时线程没有登录上下文，多租户插件不注入 tenant_id；若不逐租户切换上下文，
 * 读配置会落全局默认、写审计会丢租户归属。此查询本身就是在**无租户上下文**下跑的，
 * 故不需要 {@code @InterceptorIgnore}（插件无上下文时不注入）。</p>
 */
@Mapper
public interface MarketingTenantMapper {

    /** 有「启用中的自动化规则 / 未过期积分批次 / 未到期储值卡」的租户 */
    @Select("""
            SELECT DISTINCT tenant_id FROM (
                SELECT tenant_id FROM mkt_auto_campaign
                 WHERE deleted = 0 AND status = 1
                UNION
                SELECT tenant_id FROM mkt_points_batch
                 WHERE deleted = 0 AND status = 'ACTIVE' AND expire_time IS NOT NULL AND remaining_points > 0
                UNION
                SELECT tenant_id FROM mkt_stored_card
                 WHERE deleted = 0 AND status = 'ACTIVE' AND expire_time IS NOT NULL
            ) t WHERE tenant_id IS NOT NULL
            """)
    List<Long> selectActiveTenantIds();
}
