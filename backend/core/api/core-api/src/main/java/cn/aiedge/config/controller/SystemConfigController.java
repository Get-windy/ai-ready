package cn.aiedge.config.controller;

import cn.aiedge.config.model.ConfigChangeLog;
import cn.aiedge.config.model.SystemConfig;
import cn.aiedge.config.service.SystemConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器（系统参数页 / 支付配置页共用的 `/api/config`）
 *
 * <p><b>2026-09-18 改造</b>：端点路径与入参契约保持不变（支付配置、供应商、平台「系统配置」页
 * 都在用这套接口），改的是实现 —— 全部真实读写 {@code sys_config}（见
 * {@code SystemConfigServiceImpl} 类注释）。新增两个**只增不改**的端点：</p>
 * <ul>
 *   <li>{@code GET /api/config/nav-groups} —— 系统参数页左列 8 个纵向视图（文档 §8.1-2 逐字）；</li>
 *   <li>{@code GET /api/config/value-options} —— 枚举/多段控件的候选值字典（后端硬编码，
 *       与既有 {@code /types}、{@code /groups} 同风格）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理功能")
public class SystemConfigController {

    private final SystemConfigService configService;

    /**
     * 系统参数页的左列 8 个纵向视图（左列标签导航的**单一维度**）
     *
     * <p>逐字取自《设置模块/系统参数开发文档.md》§8.1-2（ql361 截图实测 2026-09-18）：
     * 行业设置 / 流程启用 / 单据设置 / 库存设置 / 财务设置 / 数据权限 / 消息提醒 / 其他。</p>
     *
     * <p>⚠️ 缺口：其中只有「行业设置」采到了具体配置项（§8.1-13），其余 6 个视图只采到标签名，
     * 页面照常渲染但内容为空，并显示「该视图配置项未实测」的缺口提示（不编造配置项）。</p>
     */
    private static final List<Map<String, Object>> NAV_GROUPS = List.of(
            Map.of("code", "industry", "name", "行业设置", "sortOrder", 1),
            Map.of("code", "flow", "name", "流程启用", "sortOrder", 2),
            Map.of("code", "bill", "name", "单据设置", "sortOrder", 3),
            Map.of("code", "stock", "name", "库存设置", "sortOrder", 4),
            Map.of("code", "finance", "name", "财务设置", "sortOrder", 5),
            Map.of("code", "data_perm", "name", "数据权限", "sortOrder", 6),
            Map.of("code", "notify", "name", "消息提醒", "sortOrder", 7),
            Map.of("code", "other", "name", "其他", "sortOrder", 8)
    );

    /** 小数位数下拉（数量 / 单价共用同一套选项：整数 + 1~8 位小数）——必须声明在 VALUE_OPTIONS 之前 */
    private static final List<Map<String, String>> SCALE_OPTIONS = List.of(
            option("int", "整数"), option("1", "1位小数"), option("2", "2位小数"), option("3", "3位小数"),
            option("4", "4位小数"), option("5", "5位小数"), option("6", "6位小数"), option("7", "7位小数"),
            option("8", "8位小数"));

    /**
     * 单选型配置项的候选值字典（按配置键索引）——**全部逐字取自 ql361 实测的展开下拉**，
     * 未采集到的候选值一律不编造。
     *
     * <p>2026-09-18 补录：上一轮只采到「行业设置」的 3 个下拉的当前值；本次抓完了 8 个标签的
     * 全部下拉（`tool-results/ql361/设置-deep/系统参数-下拉选项-*.json`），把完整选项集接进来。</p>
     */
    private static final Map<String, List<Map<String, String>>> VALUE_OPTIONS = Map.ofEntries(
            // ── 行业设置 ──
            // 批次商品成本规则（实测选项：按移动加权平均 / 按商品批次成本）
            Map.entry("industry.batch.costRule", List.of(
                    option("movingAverage", "按移动加权平均"),
                    option("batchCost", "按商品批次成本"))),
            // 批次保质期商品默认出库规则（实测选项：近效先出 / 手工指定）
            Map.entry("industry.batch.outboundRule", List.of(
                    option("nearExpiryFirst", "近效先出"),
                    option("manual", "手工指定"))),
            // ── 流程启用 ──
            // 默认配送方式（实测展开浮层：请选择 / 物流）
            Map.entry("flow.delivery.defaultMode", List.of(
                    option("none", "请选择"),
                    option("logistics", "物流"))),
            // ── 单据设置 ──
            Map.entry("bill.no.cycle", List.of(
                    option("day", "按日编号"), option("month", "按月编号"), option("year", "按年编号"))),
            Map.entry("bill.no.digits", List.of(
                    option("3", "3"), option("4", "4"), option("5", "5"), option("6", "6"))),
            Map.entry("bill.picking.sortRule", List.of(
                    option("entryOrder", "按录单商品顺序排列"), option("pickingOrder", "按拣货包件顺序排列"))),
            Map.entry("bill.cost.abnormalRule", List.of(
                    option("refCost", "参考成本"), option("manualCost", "手动录入成本"),
                    option("zeroCost", "按0成本出库"), option("lastPrice", "最近进价"),
                    option("presetPrice", "读取预设进价"))),
            Map.entry("bill.item.sortRule", List.of(
                    option("entryOrder", "按录单排序"), option("nameAsc", "按商品名称升序"),
                    option("nameDesc", "按商品名称降序"), option("codeAsc", "按货号升序"),
                    option("codeDesc", "按货号降序"), option("locAsc", "按货位升序"),
                    option("locDesc", "按货位降序"))),
            // 调拨单默认调拨价：实测展开后除 5 个取价项外还混入了 8 个客户等级名（ql361 自身的
            // 数据问题：客户级别档案被并进了取价下拉）—— 照实登记，不删也不改
            Map.entry("bill.transfer.defaultPrice", List.of(
                    option("cost", "成本价"), option("presetPrice", "预设进价"), option("retail", "零售价"),
                    option("minSale", "最低售价"), option("wholesale", "批发价"),
                    option("catering", "餐饮店"), option("canteen", "食堂团餐"),
                    option("outCatering", "外围餐饮店"), option("vipSelf", "自助vip"),
                    option("bigCanteen", "大团餐"), option("keyVip01", "重点|vip01"),
                    option("chainVip", "连锁|vip"), option("specialCustomer", "特价客户"))),
            Map.entry("bill.account.count", List.of(
                    option("1", "1账户"), option("2", "2账户"), option("3", "3账户"), option("4", "4账户"))),
            Map.entry("bill.qty.scale", SCALE_OPTIONS),
            Map.entry("bill.price.scale", SCALE_OPTIONS),
            Map.entry("bill.sale.creditOver", List.of(
                    option("notAllowed", "不允许开单"), option("notice", "仅提示"))),
            Map.entry("bill.purchase.priceAlertAction", List.of(
                    option("block", "不允许提交/记账"), option("notice", "仅提醒"))),
            // ── 库存设置 ──
            Map.entry("stock.alertRule", List.of(
                    option("bookGtUpper", "账面库存>库存上限"),
                    option("bookLtUpper", "账面库存<库存上限"),
                    option("bookMinusPendingGtUpper", "账面库存-待发货>库存上限"),
                    option("bookPlusIncomingMinusPendingGtUpper", "账面库存+待收货-待发货>库存上限"))),
            Map.entry("stock.replenishRule", List.of(
                    option("lowerMinusBook", "缺货数量=库存下限-账面数量"),
                    option("upperMinusBook", "缺货数量=库存上限-账面数量"),
                    option("lowerPlusPendingMinusBookMinusIncoming", "缺货数量=库存下限+待发货-账面数量-待收货"),
                    option("upperPlusPendingMinusBookMinusIncoming", "缺货数量=库存上限+待发货-账面数量-待收货"))),
            Map.entry("stock.available.formula", List.of(
                    option("bookMinusUnsoldPlusUnreceivedMinusPending", "账面库存-未发数量+未收数量-待记账数量"),
                    option("bookMinusUnsoldMinusPending", "账面库存-未发数量-待记账数量"))),
            // ── 财务设置 ──
            Map.entry("finance.cost.method", List.of(
                    option("fifo", "先进先出"), option("movingAverage", "移动加权平均"))),
            Map.entry("finance.monthClose.autoBefore", List.of(
                    option("1", "一月前"), option("2", "二月前"))),
            Map.entry("finance.payment.settleOrder", List.of(
                    option("docOrder", "按单据录单顺序由远及近"), option("unsettledAsc", "按单据未结金额从小到大"))),
            // ── 其他 ──
            Map.entry("other.share.expireDays", List.of(
                    option("1", "1天"), option("2", "2天"), option("3", "3天"),
                    option("7", "7天"), option("30", "30天"), option("forever", "永久有效"))),
            Map.entry("other.lockScreen.timeout", List.of(
                    option("5", "5分钟"), option("10", "10分钟"), option("15", "15分钟"),
                    option("30", "30分钟"), option("60", "1小时"), option("never", "永不锁定"))),
            Map.entry("other.trace.vendor", List.of(
                    option("none", "无"), option("anhui", "安徽省食品安全追溯平台")))
    );

    /**
     * **多段控件**的候选值字典（按配置键索引；外层数组 = 第 1..N 段，内层数组 = 该段的候选值）。
     *
     * <p>为什么单开一张表：ql361 的「批次条码规则生成」是 3 个**串联下拉**，第 3 段的取值域
     * 与第 1、2 段**不同**（前两段是商品字段，第 3 段是随机位数）——用一个扁平选项集表达不了，
     * 前端 `ParamControl` 对 `value_type = 'list'` 的项按段取候选值。</p>
     */
    private static final Map<String, List<List<Map<String, String>>>> SEGMENT_OPTIONS = Map.of(
            "industry.batch.barcodeRule", List.of(
                    List.of(option("none", "无"), option("productNo", "商品货号"),
                            option("baseBarcode", "基本商品条码"), option("produceDate", "生产日期"),
                            option("expireDate", "到期日期")),
                    List.of(option("none", "无"), option("productNo", "商品货号"),
                            option("baseBarcode", "基本商品条码"), option("produceDate", "生产日期"),
                            option("expireDate", "到期日期")),
                    List.of(option("none", "无"), option("rand1", "1位随机数"),
                            option("rand2", "2位随机数"), option("rand3", "3位随机数")))
    );

    private static Map<String, String> option(String value, String label) {
        return Map.of("value", value, "label", label);
    }

    /** 把请求参数装进查询载体（只用到 SystemConfig 的查询类字段） */
    private static SystemConfig buildQuery(String configType, String configGroup, String navGroup,
                                           String configKey, String configName,
                                           Boolean enabled, Boolean systemConfig) {
        SystemConfig query = new SystemConfig();
        query.setConfigType(configType);
        query.setConfigGroup(configGroup);
        query.setNavGroup(navGroup);
        query.setConfigKey(configKey);
        query.setConfigName(configName);
        query.setEnabled(enabled);
        query.setSystemConfig(systemConfig);
        return query;
    }

    @GetMapping("/page")
    @SaCheckPermission("system:config:list")
    @Operation(summary = "分页查询配置")
    public ResponseEntity<Map<String, Object>> getConfigPage(
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String configGroup,
            @RequestParam(required = false) String navGroup,
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean systemConfig,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        SystemConfig query = buildQuery(configType, configGroup, navGroup, configKey, configName, enabled, systemConfig);

        // 真分页：total 走独立 COUNT，records 走 LIMIT/OFFSET（改造前 total = 返回条数、pages 恒 1）
        long total = configService.getConfigCount(query, tenantId);
        List<SystemConfig> records = total == 0 ? List.of() : configService.getConfigPage(query, pageNum, pageSize, tenantId);
        long pages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0L;

        return ResponseEntity.ok(Map.of(
                "records", records,
                "total", total,
                "current", pageNum,
                "size", pageSize,
                "pages", pages));
    }

    @GetMapping("/list")
    @SaCheckPermission("system:config:list")
    @Operation(summary = "获取配置列表")
    public ResponseEntity<Map<String, Object>> getConfigList(
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String configGroup,
            @RequestParam(required = false) String navGroup,
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean systemConfig,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<SystemConfig> configs = configService.getConfigList(
                buildQuery(configType, configGroup, navGroup, configKey, configName, enabled, systemConfig), tenantId);
        return ResponseEntity.ok(Map.of("records", configs, "total", configs.size(), "code", 200, "message", "ok"));
    }

    @GetMapping("/nav-groups")
    @SaCheckPermission("system:config:list")
    @Operation(summary = "获取系统参数左列纵向视图（8 个）")
    public ResponseEntity<List<Map<String, Object>>> getNavGroups() {
        return ResponseEntity.ok(NAV_GROUPS);
    }

    /**
     * 候选值字典：`{ 配置键: 扁平选项集 | 分段选项集 }`
     *
     * <p>键的两种形态：单一枚举项（`[{value,label}]`）与多段控件（`[[{value,label}], ...]`，
     * 外层下标 = 段序号）。前端按「首元素是不是数组」区分，无需按配置键硬编码。</p>
     */
    @GetMapping("/value-options")
    @SaCheckPermission("system:config:list")
    @Operation(summary = "获取枚举/多段配置项的候选值字典")
    public ResponseEntity<Map<String, Object>> getValueOptions() {
        Map<String, Object> all = new LinkedHashMap<>();
        all.putAll(VALUE_OPTIONS);
        all.putAll(SEGMENT_OPTIONS);
        return ResponseEntity.ok(all);
    }

    @SaCheckPermission("system:config:list")
    @GetMapping("/map")
    @Operation(summary = "获取配置Map")
    public ResponseEntity<Map<String, String>> getConfigMap(
            @RequestParam(required = false) String configGroup,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        Map<String, String> configMap = configService.getConfigMap(configGroup, tenantId);
        return ResponseEntity.ok(configMap);
    }

    @SaCheckPermission("system:config:list")
    @GetMapping("/value/{configKey}")
    @Operation(summary = "获取配置值")
    public ResponseEntity<Map<String, Object>> getConfigValue(
            @PathVariable String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        SystemConfig config = configService.getConfigByKey(configKey, tenantId);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("key", configKey, "value", config.getConfigValue(), "config", config));
    }

    @PostMapping("/save")
    @SaCheckPermission("system:config:update")
    @Operation(summary = "保存配置")
    public ResponseEntity<Map<String, Object>> saveConfig(
            @RequestBody SystemConfig config,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        SystemConfig saved = configService.saveConfig(config, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "config", saved));
    }

    @PostMapping("/save-value")
    @SaCheckPermission("system:config:update")
    @Operation(summary = "保存配置值")
    public ResponseEntity<Map<String, Object>> saveConfigValue(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        String configKey = request.get("configKey");
        String configValue = request.get("configValue");
        configService.saveConfigValue(configKey, configValue, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "message", "保存成功"));
    }

    @PostMapping("/batch-save")
    @SaCheckPermission("system:config:update")
    @Operation(summary = "批量保存配置")
    public ResponseEntity<Map<String, Object>> batchSaveConfigs(
            @RequestBody Map<String, String> configs,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        configService.batchSaveConfigs(configs, tenantId);
        return ResponseEntity.ok(Map.of("success", true, "count", configs.size()));
    }

    @DeleteMapping("/{configKey}")
    @SaCheckPermission("system:config:delete")
    @Operation(summary = "删除配置")
    public ResponseEntity<Map<String, Object>> deleteConfig(
            @PathVariable String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        boolean success = configService.deleteConfigByKey(configKey, tenantId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @GetMapping("/logs/{configKey}")
    @SaCheckPermission("system:config:list")
    @Operation(summary = "获取配置变更日志")
    public ResponseEntity<List<ConfigChangeLog>> getConfigChangeLogs(
            @PathVariable String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        List<ConfigChangeLog> logs = configService.getConfigChangeLogs(configKey, tenantId);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/refresh-cache")
    @SaCheckPermission("system:config:update")
    @Operation(summary = "刷新配置缓存")
    public ResponseEntity<Map<String, Object>> refreshCache(
            @RequestParam(required = false) String configKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {

        // 返回 cleared = 实际清理掉的历史缓存键数量（旧实现只打一行日志，没有任何可观察效果）
        long cleared = configKey != null
                ? configService.refreshCache(configKey, tenantId)
                : configService.refreshCache(tenantId);
        return ResponseEntity.ok(Map.of("success", true, "message", "缓存刷新成功", "cleared", cleared));
    }

    @DeleteMapping("/batch")
    @SaCheckPermission("system:config:delete")
    @Operation(summary = "批量删除配置")
    public ResponseEntity<Map<String, Object>> batchDelete(
            @RequestBody List<Long> ids,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        boolean success = configService.batchDelete(ids, tenantId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @GetMapping("/export")
    @SaCheckPermission("system:config:export")
    @Operation(summary = "导出配置")
    public ResponseEntity<List<SystemConfig>> export(
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) String configGroup,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        List<SystemConfig> configs = configService.getConfigList(configType, configGroup, tenantId);
        return ResponseEntity.ok(configs);
    }

    @SaCheckPermission("system:config:list")
    @GetMapping("/types")
    @Operation(summary = "获取配置类型")
    public ResponseEntity<List<Map<String, String>>> getConfigTypes() {
        return ResponseEntity.ok(List.of(
            Map.of("code", "system", "name", "系统配置"),
            Map.of("code", "security", "name", "安全配置"),
            Map.of("code", "business", "name", "业务配置"),
            Map.of("code", "notification", "name", "通知配置"),
            Map.of("code", "integration", "name", "集成配置")
        ));
    }

    @SaCheckPermission("system:config:list")
    @GetMapping("/groups")
    @Operation(summary = "获取配置分组")
    public ResponseEntity<List<Map<String, String>>> getConfigGroups() {
        return ResponseEntity.ok(List.of(
            Map.of("code", "basic", "name", "基础配置"),
            Map.of("code", "login", "name", "登录配置"),
            Map.of("code", "password", "name", "密码配置"),
            Map.of("code", "session", "name", "会话配置"),
            Map.of("code", "upload", "name", "上传配置"),
            Map.of("code", "email", "name", "邮件配置"),
            Map.of("code", "sms", "name", "短信配置")
        ));
    }
}
