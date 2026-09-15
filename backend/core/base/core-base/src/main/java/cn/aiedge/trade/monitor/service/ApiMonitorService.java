package cn.aiedge.trade.monitor.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.monitor.dto.ApiCallQuery;
import cn.aiedge.trade.monitor.dto.ApiEndpointVO;
import cn.aiedge.trade.monitor.dto.ApiMonitorAlertVO;
import cn.aiedge.trade.monitor.dto.ApiMonitorThreshold;
import cn.aiedge.trade.monitor.dto.DependencyHealthVO;
import cn.aiedge.trade.monitor.dto.SandboxInvokeRequest;
import cn.aiedge.trade.monitor.dto.SandboxResultVO;
import cn.aiedge.trade.monitor.entity.ApiAccessLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API 监控服务（《API监控开发文档》§3 金标准目标设计）
 *
 * <p>全部指标来自**真实数据**：调用日志 `api_access_log`、库存同步记录 `inventory_sync_record`、
 * 配置中心 `dms_config`、渠道台账 `external_channel_config`。无数据即返回空/null，**不写死任何常量**。</p>
 */
public interface ApiMonitorService {

    /** 统计卡片（今日调用量/成功率/平均耗时/P95/失败数/同步失败数） */
    Map<String, Object> stat();

    /** 依赖健康逐项（DB / Redis / MQ / 地图服务 / 第三方渠道） */
    List<DependencyHealthVO> deps();

    /** 调用日志分页 */
    PageResult<ApiAccessLog> callsPage(ApiCallQuery query);

    /** 调用日志列表（导出用，最多 5000 条，避免全量导出打挂内存） */
    List<ApiAccessLog> callsList(ApiCallQuery query);

    /** 调用日志分维度统计（groupBy = channel / api / direction） */
    List<Map<String, Object>> callsStat(String groupBy, LocalDateTime from, LocalDateTime to);

    /** 调用量按小时趋势（返回 [total, fail] 双序列数据点） */
    List<Map<String, Object>> callsTrend(LocalDateTime from, LocalDateTime to);

    /** 开放接口目录（联调分组树） */
    List<ApiEndpointVO> endpoints();

    /**
     * 联调自检：回环调用本实例的真实开放接口
     *
     * @param request       接口键 + 参数
     * @param authorization 联调人的 Authorization 头（原样透传，保证与页面同权限）
     * @param baseUrl       本实例基址（http://127.0.0.1:port）
     */
    SandboxResultVO sandboxInvoke(SandboxInvokeRequest request, String authorization, String baseUrl);

    /** 异常告警（阈值判定 + 静默期） */
    List<ApiMonitorAlertVO> alerts();

    /** 当前生效阈值（含来源：配置中心 / 代码默认） */
    Map<String, Object> thresholds();

    /** 按保留策略清理调用日志（0=不清理），返回删除条数 */
    int cleanExpired();

    /** 库存同步记录统计（状态分布 + 失败原因分类） */
    Map<String, Object> syncStat();

    /** 同步失败重试（按记录 ID 重放推送，回写重试次数/结果/错误分类） */
    Map<String, Object> retrySync(Long id);
}
