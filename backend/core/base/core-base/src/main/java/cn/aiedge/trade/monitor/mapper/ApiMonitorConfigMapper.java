package cn.aiedge.trade.monitor.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API 监控所需的**跨域只读**查询（配置中心阈值 / 第三方渠道台账 / 近期失败）
 *
 * <p>均为只读聚合：</p>
 * <ul>
 *   <li>配置中心 `dms_config`：按前缀读「全局默认(tenant_id=0) + 当前租户」两行，租户行优先；
 *       全局行必须显式关闭多租户过滤（否则租户会话读不到默认值，见《配送参数开发文档》§7 同源问题），
 *       故用 {@code @InterceptorIgnore} + 显式 tenant_id 条件（只读、无越权面）。</li>
 *   <li>第三方渠道 `external_channel_config`：启用渠道清单（本页监控对象：外部订单/库存同步）。</li>
 *   <li>`api_access_log`：近 24 小时按渠道的成功/失败与最近错误摘要（依赖健康面板用）。</li>
 * </ul>
 */
@Mapper
public interface ApiMonitorConfigMapper {

    /**
     * 按前缀读配置（全局默认行 + 指定租户行，租户行优先由调用方解析）
     *
     * @param prefix   配置键前缀（如 `monitor.` / `map.`，内部补 `%`）
     * @param tenantId 当前租户（可为 0/空 → 仅全局）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT config_key AS \"configKey\", config_value AS \"configValue\", tenant_id AS \"tenantId\" "
            + "FROM dms_config WHERE deleted = 0 AND config_key LIKE #{prefix} "
            + "AND tenant_id IN (0, #{tenantId}) ORDER BY config_key, tenant_id")
    List<Map<String, Object>> selectConfigsByPrefix(@Param("prefix") String prefix,
                                                    @Param("tenantId") Long tenantId);

    /** 启用中的第三方渠道（本系统对外开放对接台账） */
    @Select("SELECT channel_code AS \"channelCode\", channel_name AS \"channelName\", "
            + "sync_enabled AS \"syncEnabled\", last_sync_time AS \"lastSyncTime\" "
            + "FROM external_channel_config WHERE deleted = 0 AND status = 1 ORDER BY channel_code")
    List<Map<String, Object>> selectEnabledChannels();

    /** 近 24 小时按渠道的调用成功/失败与最近一次失败（依赖健康：最近失败时间 + 错误摘要） */
    @Select("SELECT channel_code AS \"channelCode\", "
            + "SUM(CASE WHEN status = 'FAIL' THEN 1 ELSE 0 END)::int AS \"fail\", "
            + "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END)::int AS \"success\", "
            + "MAX(CASE WHEN status = 'FAIL' THEN access_time END) AS \"lastFailTime\" "
            + "FROM api_access_log WHERE channel_code IS NOT NULL AND access_time >= #{from} "
            + "GROUP BY channel_code")
    List<Map<String, Object>> selectRecentChannelFailures(@Param("from") LocalDateTime from);

    /** 某渠道最近一次失败的错误摘要 */
    @Select("SELECT error_msg FROM api_access_log WHERE channel_code = #{channelCode} AND status = 'FAIL' "
            + "ORDER BY access_time DESC LIMIT 1")
    String selectLastChannelError(@Param("channelCode") String channelCode);
}
