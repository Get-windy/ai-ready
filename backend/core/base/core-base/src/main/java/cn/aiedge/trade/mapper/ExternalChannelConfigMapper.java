package cn.aiedge.trade.mapper;

import cn.aiedge.trade.entity.ExternalChannelConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ExternalChannelConfigMapper extends BaseMapper<ExternalChannelConfig> {

    /**
     * 渠道台账统计（真实聚合：渠道总数 / 启用数 / 同步开启数 / 异常数）
     *
     * <p>口径说明：</p>
     * <ul>
     *   <li><b>总数</b>：未删除的渠道配置行数。</li>
     *   <li><b>启用数</b>：status = 1（1 正常 / 0 禁用）。</li>
     *   <li><b>同步开启数</b>：sync_enabled = 1。</li>
     *   <li><b>异常数</b>：已禁用（status 非 1，含 NULL）<b>或</b> access_token 已过期
     *       （token_expire_time 非空且早于当前时间）的渠道——即「不可用/需重新授权」的渠道。</li>
     * </ul>
     *
     * <p>⚠️ 手写 SQL 必须自带 `deleted = 0`：MyBatis-Plus 的逻辑删除只作用于内置方法，
     * 不会改写本注解 SQL（见《MyBatis 两个静默陷阱》）；tenant_id 条件由租户插件统一注入。</p>
     */
    @Select("SELECT COUNT(*)::int AS \"total\", "
            + "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0)::int AS \"enabledCount\", "
            + "COALESCE(SUM(CASE WHEN sync_enabled = 1 THEN 1 ELSE 0 END), 0)::int AS \"syncEnabledCount\", "
            + "COALESCE(SUM(CASE WHEN COALESCE(status, 0) <> 1 "
            + "  OR (token_expire_time IS NOT NULL AND token_expire_time < NOW()) THEN 1 ELSE 0 END), 0)::int AS \"abnormalCount\" "
            + "FROM external_channel_config WHERE deleted = 0")
    Map<String, Object> statChannels();
}
