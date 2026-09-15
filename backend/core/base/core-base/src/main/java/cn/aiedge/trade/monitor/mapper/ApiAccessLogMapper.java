package cn.aiedge.trade.monitor.mapper;

import cn.aiedge.trade.monitor.entity.ApiAccessLog;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 接口调用日志 Mapper（网关级监控：聚合 + 分页 + 留存清理）
 *
 * <p>全部为**只读聚合**（除留存清理的物理删除）：调用量、成功率、平均/P95/最大耗时、按渠道/接口/方向下钻、
 * 按小时趋势。多租户条件由租户插件统一注入（本表含 `tenant_id`），与《配送模块 README》§5 口径一致。</p>
 *
 * <p>⚠️ P95 不用 `percentile_disc ... WITHIN GROUP`（JSqlParser 解析告警风险），改为
 * 「按耗时升序取指定偏移的一行」，等价且解析安全。</p>
 */
@Mapper
public interface ApiAccessLogMapper extends BaseMapper<ApiAccessLog> {

    // ────────── 卡片口径（默认仅统计 direction IN / OUT，联调自检不计入指标） ──────────

    @Select("SELECT COUNT(*)::int FROM api_access_log "
            + "WHERE access_time >= #{from} AND access_time < #{to} AND direction IN ('IN','OUT')")
    int countInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*)::int FROM api_access_log "
            + "WHERE access_time >= #{from} AND access_time < #{to} AND direction IN ('IN','OUT') "
            + "AND status = 'SUCCESS'")
    int countSuccessInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COALESCE(AVG(response_time), 0)::int FROM api_access_log "
            + "WHERE access_time >= #{from} AND access_time < #{to} AND direction IN ('IN','OUT') "
            + "AND response_time IS NOT NULL")
    int avgCostInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COALESCE(MAX(response_time), 0)::int FROM api_access_log "
            + "WHERE access_time >= #{from} AND access_time < #{to} AND direction IN ('IN','OUT') "
            + "AND response_time IS NOT NULL")
    int maxCostInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*)::int FROM api_access_log "
            + "WHERE access_time >= #{from} AND access_time < #{to} AND direction IN ('IN','OUT') "
            + "AND response_time IS NOT NULL")
    int countWithCostInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 按耗时升序取第 offset 行（P95 = offset 为 floor(0.95 * (n-1))） */
    @Select("SELECT response_time FROM api_access_log "
            + "WHERE access_time >= #{from} AND access_time < #{to} AND direction IN ('IN','OUT') "
            + "AND response_time IS NOT NULL ORDER BY response_time ASC OFFSET #{offset} LIMIT 1")
    Integer costAtOffset(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                         @Param("offset") int offset);

    // ────────── 分维度下钻 ──────────

    @Select("SELECT COALESCE(channel_code, '(未标记渠道)') AS \"channelCode\", COUNT(*)::int AS \"total\", "
            + "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END)::int AS \"success\", "
            + "COALESCE(AVG(response_time), 0)::int AS \"avgMs\", "
            + "COALESCE(MAX(response_time), 0)::int AS \"maxMs\" "
            + "FROM api_access_log WHERE access_time >= #{from} AND access_time < #{to} "
            + "AND direction IN ('IN','OUT') GROUP BY 1 ORDER BY \"total\" DESC, 1")
    List<Map<String, Object>> groupByChannel(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT api_path AS \"apiPath\", COALESCE(api_name, api_path) AS \"apiName\", "
            + "COUNT(*)::int AS \"total\", "
            + "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END)::int AS \"success\", "
            + "COALESCE(AVG(response_time), 0)::int AS \"avgMs\", "
            + "COALESCE(MAX(response_time), 0)::int AS \"maxMs\" "
            + "FROM api_access_log WHERE access_time >= #{from} AND access_time < #{to} "
            + "AND direction IN ('IN','OUT') GROUP BY 1, 2 ORDER BY \"total\" DESC, 1")
    List<Map<String, Object>> groupByApi(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT direction AS \"direction\", COUNT(*)::int AS \"total\", "
            + "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END)::int AS \"success\" "
            + "FROM api_access_log WHERE access_time >= #{from} AND access_time < #{to} "
            + "GROUP BY 1 ORDER BY 1")
    List<Map<String, Object>> countByDirection(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT to_char(date_trunc('hour', access_time), 'MM-DD HH24:00') AS \"bucket\", "
            + "COUNT(*)::int AS \"total\", "
            + "SUM(CASE WHEN status = 'FAIL' THEN 1 ELSE 0 END)::int AS \"fail\" "
            + "FROM api_access_log WHERE access_time >= #{from} AND access_time < #{to} "
            + "AND direction IN ('IN','OUT') GROUP BY 1 ORDER BY 1")
    List<Map<String, Object>> trendByHour(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ────────── 依赖健康 / 留存 ──────────

    /** 数据库探针（依赖健康面板：DB 项） */
    @Select("SELECT 1")
    Integer ping();

    /** 曾产生调用日志的租户（留存清理按租户逐个执行，避免无租户上下文时误删全量） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DISTINCT tenant_id FROM api_access_log ORDER BY tenant_id")
    List<Long> selectDistinctTenantIds();

    @Select("SELECT COUNT(*)::int FROM api_access_log WHERE access_time < #{before}")
    int countBefore(@Param("before") LocalDateTime before);

    @Delete("DELETE FROM api_access_log WHERE access_time < #{before}")
    int deleteBefore(@Param("before") LocalDateTime before);
}
