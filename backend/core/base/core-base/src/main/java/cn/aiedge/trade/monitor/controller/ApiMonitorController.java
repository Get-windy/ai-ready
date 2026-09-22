package cn.aiedge.trade.monitor.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.monitor.TimeParsers;
import cn.aiedge.trade.monitor.dto.ApiCallQuery;
import cn.aiedge.trade.monitor.dto.ApiEndpointVO;
import cn.aiedge.trade.monitor.dto.ApiMonitorAlertVO;
import cn.aiedge.trade.monitor.dto.DependencyHealthVO;
import cn.aiedge.trade.monitor.dto.SandboxInvokeRequest;
import cn.aiedge.trade.monitor.dto.SandboxResultVO;
import cn.aiedge.trade.monitor.entity.ApiAccessLog;
import cn.aiedge.trade.monitor.XlsxExporter;
import cn.aiedge.trade.monitor.service.ApiMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * API 监控（配送 → API监控，菜单 90107 `trade:api-monitor`）
 *
 * <p>《API监控开发文档》§3.3 金标准接口清单：统计卡片、依赖健康、调用日志（分页/统计/趋势/目录）、
 * 联调沙箱、异常告警、阈值、留存清理、同步记录重试。</p>
 *
 * <p>全部指标来自真实数据（`api_access_log` / `inventory_sync_record` / `dms_config` /
 * `external_channel_config`）；无数据返回 null/空集合，**不写死任何常量**。</p>
 */
@Tag(name = "API监控")
@RestController
@RequestMapping("/api/trade/api-monitor")
@RequiredArgsConstructor
public class ApiMonitorController {

    private static final String[] CALL_EXPORT_HEADERS = {
            "调用时间", "方向", "渠道", "接口路径", "接口名称", "方法", "状态",
            "响应码", "耗时(ms)", "请求号", "错误码", "错误信息", "调用方IP"
    };

    private final ApiMonitorService apiMonitorService;

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Operation(summary = "统计卡片（今日调用量/成功率/平均耗时/P95/失败数/库存同步失败数）")
    @SaCheckPermission("trade:api-monitor:view")
    @GetMapping("/stat")
    public Result<Map<String, Object>> stat() {
        return Result.success(apiMonitorService.stat());
    }

    @Operation(summary = "依赖健康逐项（DB / Redis / MQ / 地图 / 第三方渠道）")
    @SaCheckPermission("trade:api-monitor:view")
    @GetMapping("/deps")
    public Result<List<DependencyHealthVO>> deps() {
        return Result.success(apiMonitorService.deps());
    }

    @Operation(summary = "接口调用日志分页")
    @SaCheckPermission("trade:api-monitor:list")
    @GetMapping("/calls/page")
    public Result<PageResult<ApiAccessLog>> callsPage(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        ApiCallQuery query = new ApiCallQuery(pageNum, pageSize, channelCode, apiPath, direction, status, keyword,
                TimeParsers.parse(startTime), TimeParsers.parse(endTime));
        return Result.success(apiMonitorService.callsPage(query));
    }

    @Operation(summary = "接口调用日志导出（真实 xlsx，最多 5000 条）")
    @SaCheckPermission("trade:api-monitor:export")
    @GetMapping("/calls/export")
    public void callsExport(
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        ApiCallQuery query = new ApiCallQuery(1, 5000, channelCode, apiPath, direction, status, keyword,
                TimeParsers.parse(startTime), TimeParsers.parse(endTime));
        List<String[]> rows = apiMonitorService.callsList(query).stream().map(row -> new String[]{
                text(row.getAccessTime()), text(row.getDirection()), text(row.getChannelCode()),
                text(row.getApiPath()), text(row.getApiName()), text(row.getRequestMethod()),
                text(row.getStatus()), text(row.getResponseCode()), text(row.getResponseTime()),
                text(row.getRequestId()), text(row.getErrorCode()), text(row.getErrorMsg()),
                text(row.getIpAddress())
        }).toList();
        XlsxExporter.write(response, "接口调用日志_" + java.time.LocalDate.now() + ".xlsx", "接口调用日志",
                CALL_EXPORT_HEADERS, rows);
    }

    @Operation(summary = "接口调用日志分维度统计（groupBy = channel / api / direction）")
    @SaCheckPermission("trade:api-monitor:view")
    @GetMapping("/calls/stat")
    public Result<List<Map<String, Object>>> callsStat(
            @RequestParam(defaultValue = "api") String groupBy,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(apiMonitorService.callsStat(groupBy,
                TimeParsers.startOfRange(startTime), TimeParsers.endOfRange(endTime)));
    }

    @Operation(summary = "接口调用量按小时趋势")
    @SaCheckPermission("trade:api-monitor:view")
    @GetMapping("/calls/trend")
    public Result<List<Map<String, Object>>> callsTrend(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(apiMonitorService.callsTrend(
                TimeParsers.startOfRange(startTime), TimeParsers.endOfRange(endTime)));
    }

    @Operation(summary = "开放接口目录（联调分组树 + 参数定义）")
    @SaCheckPermission("trade:api-monitor:list")
    @GetMapping("/calls/endpoints")
    public Result<List<ApiEndpointVO>> endpoints() {
        return Result.success(apiMonitorService.endpoints());
    }

    @Operation(summary = "快速联调：回环调用真实开放接口（落 SANDBOX 调用日志，即联调历史）")
    @SaCheckPermission("trade:api-monitor:execute")
    @PostMapping("/sandbox/invoke")
    public Result<SandboxResultVO> sandboxInvoke(
            @RequestBody SandboxInvokeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest servletRequest) {
        String baseUrl = "http://127.0.0.1:" + servletRequest.getLocalPort();
        return Result.success(apiMonitorService.sandboxInvoke(request, authorization, baseUrl));
    }

    @Operation(summary = "异常告警（阈值判定 + 静默期 + 事件外发）")
    @SaCheckPermission("trade:api-monitor:list")
    @GetMapping("/alerts")
    public Result<List<ApiMonitorAlertVO>> alerts() {
        return Result.success(apiMonitorService.alerts());
    }

    @Operation(summary = "当前生效阈值（含来源：配置中心 / 代码默认）")
    @SaCheckPermission("trade:api-monitor:view")
    @GetMapping("/thresholds")
    public Result<Map<String, Object>> thresholds() {
        return Result.success(apiMonitorService.thresholds());
    }

    @Operation(summary = "按保留策略清理调用日志（保留天数 0 = 不清理）")
    @SaCheckPermission("trade:api-monitor:clear")
    @PostMapping("/clean-expired")
    public Result<Map<String, Object>> cleanExpired() {
        int deleted = apiMonitorService.cleanExpired();
        return Result.success(Map.of("deleted", deleted));
    }

    @Operation(summary = "库存同步记录统计（状态分布 + 失败原因分类）")
    @SaCheckPermission("trade:api-monitor:view")
    @GetMapping("/sync/stat")
    public Result<Map<String, Object>> syncStat() {
        return Result.success(apiMonitorService.syncStat());
    }

    @Operation(summary = "同步失败重试（复用原记录回写重试次数与结果）")
    @SaCheckPermission("trade:api-monitor:retry")
    @PostMapping("/sync/{id}/retry")
    public Result<Map<String, Object>> retrySync(@PathVariable Long id) {
        return Result.success(apiMonitorService.retrySync(id));
    }
}
