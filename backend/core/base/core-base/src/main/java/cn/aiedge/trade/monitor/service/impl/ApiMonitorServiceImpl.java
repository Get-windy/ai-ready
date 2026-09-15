package cn.aiedge.trade.monitor.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.dto.ProductSyncResult;
import cn.aiedge.trade.entity.InventorySyncRecord;
import cn.aiedge.trade.mapper.InventorySyncRecordMapper;
import cn.aiedge.trade.monitor.ApiCallDirection;
import cn.aiedge.trade.monitor.ApiCallLogRecorder;
import cn.aiedge.trade.monitor.ErrorCategory;
import cn.aiedge.trade.monitor.OpenApiCatalog;
import cn.aiedge.trade.monitor.dto.ApiCallQuery;
import cn.aiedge.trade.monitor.dto.ApiEndpointParamVO;
import cn.aiedge.trade.monitor.dto.ApiEndpointVO;
import cn.aiedge.trade.monitor.dto.ApiMonitorAlertVO;
import cn.aiedge.trade.monitor.dto.ApiMonitorThreshold;
import cn.aiedge.trade.monitor.dto.DependencyHealthVO;
import cn.aiedge.trade.monitor.dto.SandboxInvokeRequest;
import cn.aiedge.trade.monitor.dto.SandboxResultVO;
import cn.aiedge.trade.monitor.entity.ApiAccessLog;
import cn.aiedge.trade.monitor.event.ApiMonitorAlertEvent;
import cn.aiedge.trade.monitor.mapper.ApiAccessLogMapper;
import cn.aiedge.trade.monitor.mapper.ApiMonitorConfigMapper;
import cn.aiedge.trade.monitor.service.ApiMonitorService;
import cn.aiedge.trade.service.InventorySyncService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API 监控服务实现（《API监控开发文档》§3）
 *
 * <p>数据源全部为真实表：调用日志 `api_access_log`、库存同步记录 `inventory_sync_record`、
 * 配置中心 `dms_config`、渠道台账 `external_channel_config`；**无数据即返回空/null，不写死常量**。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiMonitorServiceImpl implements ApiMonitorService {

    /** 联调自检的 HTTP 客户端：短超时 + 非 2xx 不抛异常（要把 4xx/5xx 响应体展示给联调人） */
    private final RestTemplate sandboxRestTemplate = buildSandboxRestTemplate();

    private final ApiAccessLogMapper apiAccessLogMapper;
    private final ApiMonitorConfigMapper configMapper;
    private final InventorySyncRecordMapper syncRecordMapper;
    private final InventorySyncService inventorySyncService;
    private final ApiCallLogRecorder apiCallLogRecorder;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    private static final String SILENCE_KEY_PREFIX = "monitor:alert:silence:";
    private static final String CONFIG_PREFIX_MONITOR = "monitor.%";
    private static final String CONFIG_PREFIX_MAP = "map.%";
    /** MQ 探针超时（毫秒）：避免依赖探测拖慢页面 */
    private static final int MQ_PROBE_TIMEOUT_MS = 1500;
    /** 依赖健康中「近 24 小时」窗口 */
    private static final int FAILURE_WINDOW_HOURS = 24;

    // ══════════════════════════════ 统计卡片 ══════════════════════════════

    @Override
    public Map<String, Object> stat() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        int total = apiAccessLogMapper.countInRange(from, to);
        int success = apiAccessLogMapper.countSuccessInRange(from, to);
        int fail = Math.max(total - success, 0);
        int costCount = apiAccessLogMapper.countWithCostInRange(from, to);

        long syncTotal = syncRecordMapper.selectCount(new LambdaQueryWrapper<>());
        long syncFailed = syncRecordMapper.selectCount(
                new LambdaQueryWrapper<InventorySyncRecord>().eq(InventorySyncRecord::getSyncStatus, 2));
        long syncPending = syncRecordMapper.selectCount(
                new LambdaQueryWrapper<InventorySyncRecord>().eq(InventorySyncRecord::getSyncStatus, 0));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayCallCount", total);
        result.put("todaySuccessCount", success);
        result.put("todayFailCount", fail);
        // 无数据时为 null —— 前端显示「-」，避免 0% 被误解为「全部失败」（严禁写死常量）
        result.put("successRate", total > 0
                ? BigDecimal.valueOf(success * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                : null);
        result.put("avgCostMs", costCount > 0 ? apiAccessLogMapper.avgCostInRange(from, to) : null);
        result.put("maxCostMs", costCount > 0 ? apiAccessLogMapper.maxCostInRange(from, to) : null);
        result.put("p95CostMs", costCount > 0 ? p95(from, to, costCount) : null);
        result.put("syncTotalCount", syncTotal);
        result.put("syncFailedCount", syncTotal > 0 ? syncFailed : null);
        result.put("syncPendingCount", syncTotal > 0 ? syncPending : null);
        // 口径说明随数据下发，页面直接展示，避免「数字对不上」的解释成本
        result.put("statFrom", from.toString());
        result.put("statTo", to.toString());
        result.put("callLogSource", "api_access_log（方向 IN 入站 / OUT 出站；联调自检 SANDBOX 不计入指标）");
        result.put("updatedAt", LocalDateTime.now().toString());
        return result;
    }

    /** P95：按耗时升序取 offset = floor(0.95 × (n-1)) 的一行（等价分位，且解析安全） */
    private int p95(LocalDateTime from, LocalDateTime to, int count) {
        int offset = (int) Math.floor(0.95 * Math.max(count - 1, 0));
        Integer value = apiAccessLogMapper.costAtOffset(from, to, offset);
        return value != null ? value : 0;
    }

    // ══════════════════════════════ 依赖健康 ══════════════════════════════

    @Override
    public List<DependencyHealthVO> deps() {
        List<DependencyHealthVO> list = new ArrayList<>();
        list.add(dbHealth());
        list.add(redisHealth());
        list.add(mqHealth());
        list.add(mapHealth());
        list.addAll(channelHealth());
        return list;
    }

    private DependencyHealthVO dbHealth() {
        long start = System.currentTimeMillis();
        try {
            apiAccessLogMapper.ping();
            return new DependencyHealthVO("db", "数据库", "DATABASE", DependencyHealthVO.UP,
                    (int) (System.currentTimeMillis() - start), "主库连通（SELECT 1 探测）", null, null);
        } catch (Exception e) {
            return new DependencyHealthVO("db", "数据库", "DATABASE", DependencyHealthVO.DOWN,
                    (int) (System.currentTimeMillis() - start), "主库探测失败",
                    LocalDateTime.now().toString(), e.getMessage());
        }
    }

    private DependencyHealthVO redisHealth() {
        long start = System.currentTimeMillis();
        try {
            StringRedisTemplate stringRedis = redisProvider.getIfAvailable();
            if (stringRedis == null) {
                return new DependencyHealthVO("redis", "Redis 缓存", "CACHE", DependencyHealthVO.NOT_CONFIGURED,
                        null, "未装配 StringRedisTemplate", null, null);
            }
            String pong = stringRedis.execute((RedisCallback<String>) RedisConnection::ping);
            boolean ok = "PONG".equalsIgnoreCase(pong);
            return new DependencyHealthVO("redis", "Redis 缓存", "CACHE",
                    ok ? DependencyHealthVO.UP : DependencyHealthVO.DOWN,
                    (int) (System.currentTimeMillis() - start), "PING/PONG 探测", null,
                    ok ? null : "返回 " + pong);
        } catch (Exception e) {
            return new DependencyHealthVO("redis", "Redis 缓存", "CACHE", DependencyHealthVO.DOWN,
                    (int) (System.currentTimeMillis() - start), "PING 探测失败",
                    LocalDateTime.now().toString(), e.getMessage());
        }
    }

    /**
     * 消息中间件：**TCP 连通性探测**（不做 AMQP 握手，如实标注探测深度）
     *
     * <p>Broker 地址取 `spring.rabbitmq.host/port`（本平台 MQ 走 RabbitMQ，配置在 core-api 的 dev 档）；
     * 未配置即 {@code NOT_CONFIGURED}（领域事件另有 dms_event_outbox 发件箱通道）。</p>
     */
    private DependencyHealthVO mqHealth() {
        String host = environment.getProperty("spring.rabbitmq.host");
        Integer port = environment.getProperty("spring.rabbitmq.port", Integer.class, 5672);
        if (!StringUtils.hasText(host)) {
            return new DependencyHealthVO("mq", "消息中间件", "MQ", DependencyHealthVO.NOT_CONFIGURED, null,
                    "未配置 MQ Broker（spring.rabbitmq.host）；领域事件走 dms_event_outbox 发件箱", null, null);
        }
        String endpoint = host.trim() + ":" + port;
        long start = System.currentTimeMillis();
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host.trim(), port), MQ_PROBE_TIMEOUT_MS);
            return new DependencyHealthVO("mq", "消息中间件", "MQ", DependencyHealthVO.UP,
                    (int) (System.currentTimeMillis() - start),
                    "Broker=" + endpoint + "；TCP 连通性探测通过（未做 AMQP 握手）", null, null);
        } catch (Exception e) {
            return new DependencyHealthVO("mq", "消息中间件", "MQ", DependencyHealthVO.DOWN,
                    (int) (System.currentTimeMillis() - start),
                    "Broker=" + endpoint + "；TCP 连通性探测失败",
                    LocalDateTime.now().toString(), e.getMessage());
        }
    }

    /** 地图服务：读配置中心 `map.*` + 应用配置 `dms.map.*`（Key 缺失即降级为直线模式，如实标注） */
    private DependencyHealthVO mapHealth() {
        ConfigSnapshot snapshot = loadConfigs(CONFIG_PREFIX_MAP);
        String provider = firstNonBlank(
                snapshot.values().get("map.default-provider"),
                environment.getProperty("dms.map.default-provider"),
                "amap");
        String serverKey = firstNonBlank(
                environment.getProperty("dms.map." + provider + ".api-key"),
                snapshot.values().get("map." + provider + ".api-key"));
        String source = StringUtils.hasText(environment.getProperty("dms.map." + provider + ".api-key"))
                ? "环境变量/application.yml"
                : (StringUtils.hasText(snapshot.values().get("map." + provider + ".api-key")) ? "配置中心" : null);
        String jsKey = firstNonBlank(environment.getProperty("dms.map." + provider + ".js-key"),
                snapshot.values().get("map." + provider + ".js-key"));

        if (serverKey == null) {
            return new DependencyHealthVO("map", "地图服务", "MAP", DependencyHealthVO.NOT_CONFIGURED, null,
                    "服务商=" + providerLabel(provider) + "；未配置 API Key → 路径规划降级为直线距离（建单不受影响）",
                    null, null);
        }
        return new DependencyHealthVO("map", "地图服务", "MAP", DependencyHealthVO.UP, null,
                "服务商=" + providerLabel(provider) + "；Key 来源=" + source
                        + (jsKey != null ? "；前端底图 Key 已配置" : "；前端底图未配置（回退矢量画布）"),
                null, null);
    }

    /** 第三方渠道：启用渠道逐项 + 近 24 小时调用成功/失败与最近失败摘要 */
    private List<DependencyHealthVO> channelHealth() {
        List<Map<String, Object>> channels = configMapper.selectEnabledChannels();
        LocalDateTime windowStart = LocalDateTime.now().minusHours(FAILURE_WINDOW_HOURS);
        Map<String, Map<String, Object>> failures = new HashMap<>();
        for (Map<String, Object> row : configMapper.selectRecentChannelFailures(windowStart)) {
            failures.put(str(row.get("channelCode")), row);
        }

        List<DependencyHealthVO> list = new ArrayList<>();
        for (Map<String, Object> channel : channels) {
            String code = str(channel.get("channelCode"));
            String name = str(channel.get("channelName"));
            Map<String, Object> recent = failures.get(code);
            int fail = recent == null ? 0 : intOf(recent.get("fail"));
            int ok = recent == null ? 0 : intOf(recent.get("success"));
            boolean down = fail > 0 && ok == 0;
            String lastFailTime = recent == null ? null : str(recent.get("lastFailTime"));
            String lastError = down ? configMapper.selectLastChannelError(code) : null;
            String detail = (intOf(channel.get("syncEnabled")) == 1 ? "已启用库存同步" : "已启用（未开启同步）")
                    + "；近 " + FAILURE_WINDOW_HOURS + " 小时调用 成功 " + ok + " / 失败 " + fail;
            list.add(new DependencyHealthVO("channel:" + code, name == null ? code : name, "CHANNEL",
                    down ? DependencyHealthVO.DOWN : DependencyHealthVO.UP, null, detail,
                    down ? lastFailTime : null, lastError));
        }
        return list;
    }

    private String providerLabel(String provider) {
        return switch (provider == null ? "" : provider) {
            case "amap" -> "高德";
            case "tencent" -> "腾讯";
            case "baidu" -> "百度";
            default -> provider;
        };
    }

    // ══════════════════════════════ 调用日志 ══════════════════════════════

    @Override
    public PageResult<ApiAccessLog> callsPage(ApiCallQuery query) {
        long pageNum = query.pageNum() <= 0 ? 1 : query.pageNum();
        long pageSize = query.pageSize() <= 0 ? 20 : Math.min(query.pageSize(), 200);
        Page<ApiAccessLog> page = apiAccessLogMapper.selectPage(
                new Page<>(pageNum, pageSize), buildCallWrapper(query));
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<ApiAccessLog> callsList(ApiCallQuery query) {
        return apiAccessLogMapper.selectList(buildCallWrapper(query).last("LIMIT 5000"));
    }

    private LambdaQueryWrapper<ApiAccessLog> buildCallWrapper(ApiCallQuery query) {
        LambdaQueryWrapper<ApiAccessLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.channelCode()), ApiAccessLog::getChannelCode, query.channelCode());
        wrapper.like(StringUtils.hasText(query.apiPath()), ApiAccessLog::getApiPath, query.apiPath());
        if (ApiCallDirection.isValid(query.direction())) {
            wrapper.eq(ApiAccessLog::getDirection, query.direction().toUpperCase());
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(ApiAccessLog::getStatus, query.status().toUpperCase());
        }
        wrapper.ge(query.startTime() != null, ApiAccessLog::getAccessTime, query.startTime());
        wrapper.lt(query.endTime() != null, ApiAccessLog::getAccessTime, query.endTime());
        if (StringUtils.hasText(query.keyword())) {
            String kw = query.keyword().trim();
            wrapper.and(w -> w.like(ApiAccessLog::getRequestId, kw)
                    .or().like(ApiAccessLog::getErrorMsg, kw)
                    .or().like(ApiAccessLog::getApiName, kw)
                    .or().like(ApiAccessLog::getChannelCode, kw));
        }
        wrapper.orderByDesc(ApiAccessLog::getAccessTime).orderByDesc(ApiAccessLog::getId);
        return wrapper;
    }

    @Override
    public List<Map<String, Object>> callsStat(String groupBy, LocalDateTime from, LocalDateTime to) {
        String normalized = groupBy == null ? "api" : groupBy.trim().toLowerCase();
        List<Map<String, Object>> rows = switch (normalized) {
            case "channel" -> apiAccessLogMapper.groupByChannel(from, to);
            case "direction" -> apiAccessLogMapper.countByDirection(from, to);
            default -> apiAccessLogMapper.groupByApi(from, to);
        };
        // 成功率在 Java 侧计算，避免 SQL 除零与精度口径分叉
        for (Map<String, Object> row : rows) {
            int total = intOf(row.get("total"));
            int success = intOf(row.get("success"));
            row.put("fail", Math.max(total - success, 0));
            row.put("successRate", total > 0
                    ? BigDecimal.valueOf(success * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                    : null);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> callsTrend(LocalDateTime from, LocalDateTime to) {
        return apiAccessLogMapper.trendByHour(from, to);
    }

    @Override
    public List<ApiEndpointVO> endpoints() {
        return OpenApiCatalog.endpoints();
    }

    // ══════════════════════════════ 联调沙箱 ══════════════════════════════

    @Override
    public SandboxResultVO sandboxInvoke(SandboxInvokeRequest request, String authorization, String baseUrl) {
        String requestId = apiCallLogRecorder.newRequestId();
        LocalDateTime now = LocalDateTime.now();
        if (request == null || !StringUtils.hasText(request.apiKey())) {
            return new SandboxResultVO(requestId, null, null, null, baseUrl, 0, false, null, 0,
                    null, "缺少接口键 apiKey", now.toString());
        }
        Optional<ApiEndpointVO> found = OpenApiCatalog.byKey(request.apiKey());
        if (found.isEmpty()) {
            return new SandboxResultVO(requestId, request.apiKey(), null, null, baseUrl, 0, false, null, 0,
                    null, "未登记的接口: " + request.apiKey(), now.toString());
        }
        ApiEndpointVO endpoint = found.get();
        Map<String, String> params = request.params() == null ? Map.of() : request.params();

        // 参数装配：path 变量替换 / query 拼接 / body 原文透传；必填缺省用示例值兜底（联调便利）
        String path = endpoint.path();
        Map<String, String> queryParams = new LinkedHashMap<>();
        String body = null;
        for (ApiEndpointParamVO param : endpoint.params()) {
            String value = params.get(param.name());
            boolean blank = !StringUtils.hasText(value);
            if (blank) {
                if (param.required() && StringUtils.hasText(param.sample())) {
                    value = param.sample();
                } else {
                    continue;
                }
            }
            switch (param.in()) {
                case "path" -> path = path.replace("{" + param.name() + "}", value.trim());
                case "query" -> queryParams.put(param.name(), value.trim());
                case "body" -> body = value;
                default -> { /* 未知位置：忽略 */ }
            }
        }
        if (path.contains("{")) {
            SandboxResultVO missing = new SandboxResultVO(requestId, endpoint.key(), endpoint.name(),
                    endpoint.method(), baseUrl + path, 0, false, null, 0, null,
                    "缺少必填路径参数: " + path, now.toString());
            recordSandbox(missing, endpoint);
            return missing;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + path);
        queryParams.forEach(builder::queryParam);
        String url = builder.build().encode().toUriString();

        SandboxResultVO result;
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            // 显式要求 JSON：否则会被服务端内容协商成 XML（jackson-dataformat-xml 在类路径上），
            // 联调结果就不是「真实对接方看到的报文」
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (body != null) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            if (StringUtils.hasText(authorization)) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = sandboxRestTemplate.exchange(
                    java.net.URI.create(url), HttpMethod.valueOf(endpoint.method()), entity, String.class);
            int costMs = (int) Math.max(System.currentTimeMillis() - start, 0);
            String responseBody = apiCallLogRecorder.maskText(response.getBody());
            Integer bizCode = extractBizCode(response.getBody());
            boolean success = response.getStatusCode().value() < 400 && (bizCode == null || bizCode == 200);
            result = new SandboxResultVO(requestId, endpoint.key(), endpoint.name(), endpoint.method(), url,
                    response.getStatusCode().value(), success, bizCode, costMs, responseBody,
                    success ? null : "HTTP " + response.getStatusCode().value()
                            + (bizCode != null ? " / 业务码 " + bizCode : ""),
                    now.toString());
        } catch (Exception e) {
            result = new SandboxResultVO(requestId, endpoint.key(), endpoint.name(), endpoint.method(), url, 0, false,
                    null, (int) Math.max(System.currentTimeMillis() - start, 0), null,
                    "回环调用失败: " + e.getMessage(), now.toString());
        }
        recordSandbox(result, endpoint);
        return result;
    }

    /** 联调自检同样落调用日志（direction=SANDBOX）——页面「联调历史」即该方向的分页 */
    private void recordSandbox(SandboxResultVO result, ApiEndpointVO endpoint) {
        ApiAccessLog row = apiCallLogRecorder.build(
                ApiCallDirection.SANDBOX.name(), null, endpoint == null ? null : endpoint.path(),
                endpoint == null ? result.apiName() : endpoint.name(),
                endpoint == null ? result.method() : endpoint.method(),
                result.success(), result.httpStatus() == 0 ? null : result.httpStatus(), result.costMs(),
                result.success() ? null : "SANDBOX_" + (result.httpStatus() == 0 ? "000" : result.httpStatus()),
                result.errorMsg());
        row.setRequestId(result.requestId());
        row.setRequestParams(apiCallLogRecorder.maskQueryString(queryOf(result.url())));
        row.setAccessTime(LocalDateTime.now());
        apiCallLogRecorder.record(row);
    }

    private String queryOf(String url) {
        if (url == null) {
            return null;
        }
        int idx = url.indexOf('?');
        return idx < 0 || idx == url.length() - 1 ? null : url.substring(idx + 1);
    }

    private Integer extractBizCode(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode code = node.get("code");
            return code != null && code.isNumber() ? code.asInt() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static RestTemplate buildSandboxRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        RestTemplate template = new RestTemplate(factory);
        // 非 2xx 不抛异常：联调需要看到 4xx/5xx 的真实响应体
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }

    // ══════════════════════════════ 告警 ══════════════════════════════

    @Override
    public List<ApiMonitorAlertVO> alerts() {
        ApiMonitorThreshold threshold = threshold();
        int silenceMinutes = threshold.silenceMinutes();
        Map<String, Object> stat = stat();
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        String scope = tenantId == null ? "0" : String.valueOf(tenantId);
        List<ApiMonitorAlertVO> alerts = new ArrayList<>();

        int total = intOf(stat.get("todayCallCount"));
        if (total > 0) {
            BigDecimal errorRate = BigDecimal.valueOf(intOf(stat.get("todayFailCount")) * 100.0 / total)
                    .setScale(2, RoundingMode.HALF_UP);
            if (errorRate.compareTo(threshold.errorRatePercent()) > 0) {
                boolean critical = errorRate.compareTo(threshold.errorRatePercent().multiply(BigDecimal.valueOf(2))) > 0;
                alerts.add(alert("ERROR_RATE", critical ? "CRITICAL" : "WARN", "错误率超阈值",
                        "今日错误率 " + errorRate + "% > 阈值 " + threshold.errorRatePercent() + "%",
                        errorRate.toPlainString(), threshold.errorRatePercent().toPlainString(), "%",
                        "排查失败接口（切到「接口调用日志」按状态=FAIL 过滤），必要时降级渠道", scope, silenceMinutes));
            }
            Object p95Value = stat.get("p95CostMs");
            if (p95Value != null) {
                int p95 = intOf(p95Value);
                if (p95 > threshold.p95Ms()) {
                    alerts.add(alert("P95_LATENCY", "WARN", "P95 耗时超阈值",
                            "今日 P95 " + p95 + "ms > 阈值 " + threshold.p95Ms() + "ms",
                            String.valueOf(p95), String.valueOf(threshold.p95Ms()), "ms",
                            "按接口维度下钻定位慢接口，检查下游渠道响应", scope, silenceMinutes));
                }
            }
            int fail = intOf(stat.get("todayFailCount"));
            if (fail > threshold.failCount()) {
                alerts.add(alert("FAIL_COUNT", "WARN", "失败次数超阈值",
                        "今日失败 " + fail + " 次 > 阈值 " + threshold.failCount() + " 次",
                        String.valueOf(fail), String.valueOf(threshold.failCount()), "次",
                        "关注错误分类分布（网络/鉴权/限流/业务拒绝）", scope, silenceMinutes));
            }
        }

        Object syncFailedValue = stat.get("syncFailedCount");
        if (syncFailedValue != null) {
            int syncFailed = intOf(syncFailedValue);
            if (syncFailed > threshold.syncFailCount()) {
                alerts.add(alert("SYNC_FAILED", "WARN", "库存同步存在失败",
                        "库存同步失败 " + syncFailed + " 条 > 阈值 " + threshold.syncFailCount() + " 条",
                        String.valueOf(syncFailed), String.valueOf(threshold.syncFailCount()), "条",
                        "在「库存同步记录」Tab 按失败过滤并逐条重试", scope, silenceMinutes));
            }
        }

        for (DependencyHealthVO dep : deps()) {
            if (DependencyHealthVO.DOWN.equals(dep.status())) {
                alerts.add(alert("DEPENDENCY", "CRITICAL", "依赖异常：" + dep.name(),
                        dep.detail() + (dep.lastError() == null ? "" : "；最近错误：" + dep.lastError()),
                        "DOWN", "UP", null, "检查 " + dep.name() + " 连通性与凭据配置",
                        scope + ":" + dep.key(), silenceMinutes));
            }
        }
        return alerts;
    }

    /**
     * 构造告警并**按静默期外发事件**（同类型告警在静默期内只发一次）
     *
     * @param silenceScope  静默键作用域（租户；依赖类告警追加依赖键）
     * @param silenceMinutes 静默期（0 = 不静默）
     */
    private ApiMonitorAlertVO alert(String alertType, String level, String title, String detail,
                                    String currentValue, String threshold, String unit,
                                    String suggestion, String silenceScope, int silenceMinutes) {
        boolean silenced = markSilence(alertType, silenceScope, silenceMinutes);
        if (!silenced) {
            try {
                eventPublisher.publishEvent(new ApiMonitorAlertEvent(
                        MyBatisPlusConfig.getCurrentTenantIdValue(), alertType, level, title, detail,
                        currentValue, threshold, unit, LocalDateTime.now()));
            } catch (Exception e) {
                log.warn("[API监控] 告警事件外发失败: type={}, err={}", alertType, e.getMessage());
            }
        }
        return new ApiMonitorAlertVO(alertType, level, title, detail, currentValue, threshold, unit, silenced, suggestion);
    }

    /** 返回 true 表示「静默期内已发过，本次不再外发」 */
    private boolean markSilence(String alertType, String silenceScope, int minutes) {
        if (minutes <= 0) {
            return false;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String key = SILENCE_KEY_PREFIX + silenceScope + ":" + alertType;
            Boolean first = redis.opsForValue().setIfAbsent(key, String.valueOf(System.currentTimeMillis()),
                    Duration.ofMinutes(minutes));
            return Boolean.FALSE.equals(first);
        } catch (Exception e) {
            log.debug("[API监控] 告警静默判定失败（按未静默处理）: {}", e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════ 阈值 / 清理 / 同步记录 ══════════════════════════════

    @Override
    public Map<String, Object> thresholds() {
        ConfigSnapshot snapshot = loadConfigs(CONFIG_PREFIX_MONITOR);
        ApiMonitorThreshold threshold = ApiMonitorThreshold.from(snapshot.values());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("errorRatePercent", threshold.errorRatePercent());
        out.put("p95Ms", threshold.p95Ms());
        out.put("failCount", threshold.failCount());
        out.put("syncFailCount", threshold.syncFailCount());
        out.put("silenceMinutes", threshold.silenceMinutes());
        out.put("autoRefreshSeconds", threshold.autoRefreshSeconds());
        out.put("retentionDays", threshold.retentionDays());
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put(ApiMonitorThreshold.KEY_ERROR_RATE, sourceOf(snapshot, ApiMonitorThreshold.KEY_ERROR_RATE));
        sources.put(ApiMonitorThreshold.KEY_P95, sourceOf(snapshot, ApiMonitorThreshold.KEY_P95));
        sources.put(ApiMonitorThreshold.KEY_FAIL_COUNT, sourceOf(snapshot, ApiMonitorThreshold.KEY_FAIL_COUNT));
        sources.put(ApiMonitorThreshold.KEY_SYNC_FAIL, sourceOf(snapshot, ApiMonitorThreshold.KEY_SYNC_FAIL));
        sources.put(ApiMonitorThreshold.KEY_SILENCE, sourceOf(snapshot, ApiMonitorThreshold.KEY_SILENCE));
        sources.put(ApiMonitorThreshold.KEY_AUTO_REFRESH, sourceOf(snapshot, ApiMonitorThreshold.KEY_AUTO_REFRESH));
        sources.put(ApiMonitorThreshold.KEY_RETENTION, sourceOf(snapshot, ApiMonitorThreshold.KEY_RETENTION));
        out.put("sources", sources);
        out.put("configGroup", "配送参数 → API监控（配置中心 dms_config）");
        return out;
    }

    /** 生效阈值（内部使用；配置中心租户覆盖 > 全局默认 > 代码默认） */
    private ApiMonitorThreshold threshold() {
        return ApiMonitorThreshold.from(loadConfigs(CONFIG_PREFIX_MONITOR).values());
    }

    private String sourceOf(ConfigSnapshot snapshot, String key) {
        String configured = snapshot.sources().get(key);
        return configured == null ? "DEFAULT" : configured;
    }

    @Override
    public int cleanExpired() {
        int days = threshold().retentionDays();
        if (days <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = apiAccessLogMapper.deleteBefore(cutoff);
        if (deleted > 0) {
            log.info("[API监控] 调用日志留存清理完成，保留 {} 天，删除 {} 条", days, deleted);
        }
        return deleted;
    }

    @Override
    public Map<String, Object> syncStat() {
        long total = syncRecordMapper.selectCount(new LambdaQueryWrapper<>());
        long success = syncRecordMapper.selectCount(
                new LambdaQueryWrapper<InventorySyncRecord>().eq(InventorySyncRecord::getSyncStatus, 1));
        long failed = syncRecordMapper.selectCount(
                new LambdaQueryWrapper<InventorySyncRecord>().eq(InventorySyncRecord::getSyncStatus, 2));
        long pending = syncRecordMapper.selectCount(
                new LambdaQueryWrapper<InventorySyncRecord>().eq(InventorySyncRecord::getSyncStatus, 0));

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map<String, Object> row : syncRecordMapper.countByErrorCategory()) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            String category = str(row.get("category"));
            item.put("label", labelOf(category));
            categories.add(item);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("successCount", success);
        out.put("failedCount", failed);
        out.put("pendingCount", pending);
        out.put("errorCategories", categories);
        return out;
    }

    private String labelOf(String category) {
        if (!StringUtils.hasText(category)) {
            return ErrorCategory.UNKNOWN.getLabel();
        }
        try {
            return ErrorCategory.valueOf(category).getLabel();
        } catch (IllegalArgumentException e) {
            return category;
        }
    }

    @Override
    public Map<String, Object> retrySync(Long id) {
        InventorySyncRecord record = syncRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("同步记录不存在: " + id);
        }
        if (Integer.valueOf(1).equals(record.getSyncStatus())) {
            return Map.of("success", false, "id", id, "message", "该记录已同步成功，无需重试");
        }
        // 重试**复用原记录**（不再新增一条同步记录），仅回写重试次数与结果
        ProductSyncResult result = inventorySyncService.invokeChannel(
                record.getChannelCode(), record.getSkuCode(), record.getSyncQty());
        boolean success = result != null && Integer.valueOf(1).equals(result.getStatus());
        int retryCount = (record.getRetryCount() == null ? 0 : record.getRetryCount()) + 1;
        String errorMsg = success || result == null ? (result == null ? "渠道无返回" : null) : result.getErrorMsg();
        String category = success ? null : ErrorCategory.classify(errorMsg).name();

        LambdaUpdateWrapper<InventorySyncRecord> update = new LambdaUpdateWrapper<>();
        update.eq(InventorySyncRecord::getId, id)
                .set(InventorySyncRecord::getRetryCount, retryCount)
                .set(InventorySyncRecord::getLastRetryTime, LocalDateTime.now())
                .set(InventorySyncRecord::getSyncStatus, success ? 1 : 2)
                .set(InventorySyncRecord::getErrorMsg, errorMsg)
                .set(InventorySyncRecord::getErrorCategory, category);
        syncRecordMapper.update(null, update);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("success", success);
        out.put("retryCount", retryCount);
        out.put("syncStatus", success ? 1 : 2);
        out.put("errorCategory", category);
        out.put("errorCategoryLabel", category == null ? null : labelOf(category));
        out.put("errorMsg", errorMsg);
        out.put("message", success ? "重试成功" : "重试失败：" + errorMsg);
        return out;
    }

    // ══════════════════════════════ 配置读取（全局默认 + 租户覆盖） ══════════════════════════════

    /** 配置快照：生效值 + 来源（TENANT / GLOBAL），未配置的键不出现（调用方回落代码默认值） */
    private record ConfigSnapshot(Map<String, String> values, Map<String, String> sources) {
    }

    private ConfigSnapshot loadConfigs(String prefix) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        List<Map<String, Object>> rows = configMapper.selectConfigsByPrefix(
                prefix, tenantId == null ? 0L : tenantId);
        Map<String, String> values = new HashMap<>();
        Map<String, String> sources = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String key = str(row.get("configKey"));
            String value = str(row.get("configValue"));
            if (key == null || !StringUtils.hasText(value)) {
                // 空值视为「未配置」→ 回落到全局/代码默认，避免把空串当阈值
                continue;
            }
            Long rowTenantId = longOf(row.get("tenantId"));
            values.put(key, value.trim());
            sources.put(key, rowTenantId != null && rowTenantId != 0L ? "TENANT" : "GLOBAL");
        }
        return new ConfigSnapshot(values, sources);
    }

    // ══════════════════════════════ 小工具 ══════════════════════════════

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Long longOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
