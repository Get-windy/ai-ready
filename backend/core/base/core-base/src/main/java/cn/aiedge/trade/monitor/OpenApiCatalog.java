package cn.aiedge.trade.monitor;

import cn.aiedge.trade.monitor.dto.ApiEndpointParamVO;
import cn.aiedge.trade.monitor.dto.ApiEndpointVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 对外开放接口目录（`/api/open/**` 的实际契约）
 *
 * <p>用途：
 * <ol>
 *   <li>「快速联调」面板的**接口分组树 + 参数表单 + 示例请求**（`GET /api/trade/api-monitor/calls/endpoints`）；</li>
 *   <li>调用日志的 **api_name 反查**（URI 模板 → 中文名）。</li>
 * </ol>
 *
 * <p>⚠️ 本目录是 {@code OpenApiController} 的**人工镜像**（Spring 只能在运行期给出路径，
 * 给不出「中文名 / 参数中文名 / 示例值」这类联调所需语义）。新增开放接口时须同步登记，
 * 漏登记不会影响埋点（拦截器按 URI 模板记录，未登记时名称回退为方法名）。</p>
 */
public final class OpenApiCatalog {

    /** 分组键 → 中文名（顺序即页面分组树顺序） */
    public static final Map<String, String> GROUPS = new LinkedHashMap<>();

    private static final List<ApiEndpointVO> ENDPOINTS = new ArrayList<>();

    static {
        GROUPS.put("order", "订单接入");
        GROUPS.put("inventory", "库存查询");
        GROUPS.put("sync", "库存同步");
        GROUPS.put("product", "商品同步");
        GROUPS.put("health", "服务健康");

        // ── 订单接入 ──
        add("order", "orderCallback", "订单回调", "POST", "/api/open/order/callback/{channelCode}",
                "接收外部平台订单回调（异步入库）",
                ApiEndpointParamVO.path("channelCode", "渠道编码", true, "TAOBAO", "如 TAOBAO / JD / PDD"),
                ApiEndpointParamVO.query("signature", "签名", false, "", "平台回调签名（可空）"),
                ApiEndpointParamVO.body("callbackData", "回调报文", true,
                        "{\"externalOrderId\":\"TB-DEMO-001\",\"orderAmount\":99.00}", "平台原始 JSON 报文"));
        add("order", "orderBatch", "批量推送订单", "POST", "/api/open/order/batch/{channelCode}",
                "外部系统批量推送订单（逐条入库并返回成功数）",
                ApiEndpointParamVO.path("channelCode", "渠道编码", true, "TAOBAO", "如 TAOBAO / JD / PDD"),
                ApiEndpointParamVO.body("orderDataList", "订单报文数组", true,
                        "[\"{\\\"externalOrderId\\\":\\\"TB-DEMO-002\\\"}\"]", "JSON 字符串数组"));
        add("order", "orderStatus", "订单状态推送", "POST", "/api/open/order/status/{internalOrderId}",
                "推送订单状态变更到外部平台",
                ApiEndpointParamVO.path("internalOrderId", "内部订单ID", true, "1", "内部订单主键"),
                ApiEndpointParamVO.query("channelCode", "渠道编码", true, "TAOBAO", "目标渠道"),
                ApiEndpointParamVO.query("externalOrderId", "外部订单号", true, "TB-DEMO-001", "平台订单号"),
                ApiEndpointParamVO.query("status", "状态", true, "SHIPPED", "如 SHIPPED"));
        add("order", "pendingCount", "待处理订单数量", "GET", "/api/open/order/pending-count",
                "查询待处理订单数量（页面「待处理订单」卡同源）",
                ApiEndpointParamVO.query("channelCode", "渠道编码", false, "", "留空=全部渠道"));

        // ── 库存查询 ──
        add("inventory", "queryInventory", "库存查询", "GET", "/api/open/inventory/query",
                "查询 SKU 可用库存（30 秒缓存）",
                ApiEndpointParamVO.query("skuCode", "SKU编码", true, "SKU001", "商品 SKU"),
                ApiEndpointParamVO.queryNumber("warehouseId", "仓库ID", false, "", "留空=全仓合计"));
        add("inventory", "batchQueryInventory", "批量库存查询", "POST", "/api/open/inventory/batch-query",
                "批量查询多个 SKU 库存",
                ApiEndpointParamVO.queryNumber("warehouseId", "仓库ID", false, "", "留空=全仓合计"),
                ApiEndpointParamVO.body("skuCodes", "SKU编码数组", true, "[\"SKU001\",\"SKU002\"]", "JSON 字符串数组"));
        add("inventory", "lockInventory", "库存锁定查询", "POST", "/api/open/inventory/lock",
                "查询并校验库存是否满足锁定数量",
                ApiEndpointParamVO.query("skuCode", "SKU编码", true, "SKU001", "商品 SKU"),
                ApiEndpointParamVO.queryNumber("quantity", "锁定数量", true, "1", "大于 0"),
                ApiEndpointParamVO.queryNumber("warehouseId", "仓库ID", false, "", "留空=全仓合计"),
                ApiEndpointParamVO.query("lockId", "锁单号", true, "LOCK-DEMO-001", "调用方幂等键"));
        add("inventory", "releaseInventory", "库存释放", "POST", "/api/open/inventory/release",
                "释放已锁定库存",
                ApiEndpointParamVO.query("lockId", "锁单号", true, "LOCK-DEMO-001", "锁定时的幂等键"));

        // ── 库存同步 ──
        add("sync", "syncInventory", "库存同步推送", "POST", "/api/open/inventory/sync/{channelCode}",
                "推送库存变更到外部平台（写「库存同步记录」）",
                ApiEndpointParamVO.path("channelCode", "渠道编码", true, "TAOBAO", "已接入：TAOBAO / ERP_API"),
                ApiEndpointParamVO.query("skuCode", "SKU编码", true, "SKU001", "商品 SKU"),
                ApiEndpointParamVO.queryNumber("quantity", "同步数量", true, "100", "整数"));
        add("sync", "batchSyncInventory", "批量库存同步", "POST", "/api/open/inventory/batch-sync/{channelCode}",
                "批量推送库存到外部平台",
                ApiEndpointParamVO.path("channelCode", "渠道编码", true, "TAOBAO", "已接入：TAOBAO / ERP_API"),
                ApiEndpointParamVO.body("skuQuantities", "SKU-数量映射", true,
                        "{\"SKU001\":100,\"SKU002\":50}", "JSON 对象"));

        // ── 商品同步 ──
        add("product", "updatePrice", "商品价格更新", "POST", "/api/open/product/price/{channelCode}",
                "更新商品价格到外部平台",
                ApiEndpointParamVO.path("channelCode", "渠道编码", true, "TAOBAO", "已接入：TAOBAO / ERP_API"),
                ApiEndpointParamVO.query("skuCode", "SKU编码", true, "SKU001", "商品 SKU"),
                ApiEndpointParamVO.queryNumber("price", "价格", true, "88.50", "大于等于 0"));

        // ── 服务健康 ──
        add("health", "health", "API健康检查", "GET", "/api/open/health",
                "验证开放 API 可用性（页面「API状态」卡同源）",
                ApiEndpointParamVO.query("probe", "探针标记", false, "", "任意值，仅便于区分联调记录"));
    }

    private OpenApiCatalog() {
    }

    private static void add(String group, String key, String name, String method, String path,
                            String description, ApiEndpointParamVO... params) {
        ENDPOINTS.add(new ApiEndpointVO(group, GROUPS.get(group), key, name, method, path, description, List.of(params)));
    }

    /** 全部开放接口（页面分组树数据源） */
    public static List<ApiEndpointVO> endpoints() {
        return List.copyOf(ENDPOINTS);
    }

    /** 按接口键取目录项 */
    public static Optional<ApiEndpointVO> byKey(String key) {
        return ENDPOINTS.stream().filter(e -> e.key().equals(key)).findFirst();
    }

    /**
     * URI（模板或实际路径）→ 中文名（未登记返回空）
     *
     * <p>按**路径段**逐段比对（含 `{var}` 的段视为通配），避免 `/inventory/query` 误匹配
     * `/inventory/batch-query` 这类同前缀不同接口。</p>
     */
    public static Optional<String> nameOfPath(String pathOrTemplate) {
        if (pathOrTemplate == null || pathOrTemplate.isBlank()) {
            return Optional.empty();
        }
        String actual = stripQuery(pathOrTemplate);
        return ENDPOINTS.stream()
                .filter(e -> matches(e.path(), actual))
                .map(ApiEndpointVO::name)
                .findFirst();
    }

    private static String stripQuery(String path) {
        int idx = path.indexOf('?');
        return idx < 0 ? path : path.substring(0, idx);
    }

    private static boolean matches(String pattern, String actual) {
        String[] p = pattern.split("/");
        String[] a = actual.split("/");
        if (p.length != a.length) {
            return false;
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i].startsWith("{")) {
                continue;
            }
            if (!p[i].equals(a[i])) {
                return false;
            }
        }
        return true;
    }
}
