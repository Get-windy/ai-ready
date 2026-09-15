package cn.aiedge.dms.config.dto;

import cn.aiedge.dms.config.dto.DmsConfigMeta.EnumOption;
import cn.aiedge.dms.config.dto.DmsConfigMeta.ValueType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配置项元数据注册表（《配送参数开发文档》§3.1 / §3.6.1「元数据 + 代码同源」）
 *
 * <p>全量登记 DMS 各域实际在用的配置键（地图 / 派单策略 / 配送跟踪 / 签收 / 结算收款 / 实名与核验 / 能耗），
 * 前端据此按类型渲染控件，后端据此做**类型与范围校验**、提供**默认值恢复**。
 * 未登记的键不丢弃：由 {@link cn.aiedge.dms.config.service.ConfigService} 归入「其他」分组、按 TEXT 处理。</p>
 *
 * <p>⚠️ 新增配置键时请同时在此登记（并用迁移预置默认值），避免页面与消费方脱钩。</p>
 *
 * @author AI-Ready Team
 */
public final class DmsConfigMetaRegistry {

    /** 分组：键 → 名称（分组树顺序即此顺序） */
    public static final Map<String, String> GROUPS = new LinkedHashMap<>();

    private static final Map<String, DmsConfigMeta> REGISTRY = new LinkedHashMap<>();

    static {
        GROUPS.put("DISPATCH", "派单策略");
        GROUPS.put("TRACKING", "配送跟踪");
        GROUPS.put("SIGN", "签收");
        GROUPS.put("SETTLEMENT", "结算与收款");
        GROUPS.put("VERIFICATION", "实名与核验");
        GROUPS.put("ENERGY", "能耗与补能");
        GROUPS.put("CHANNEL", "渠道对接");
        GROUPS.put("MAP", "地图与位置服务");
        GROUPS.put("MONITOR", "API监控");
        GROUPS.put("OTHER", "其他");

        // ── 派单策略（智能调度读取）──
        enumOf("DISPATCH", "dms.dispatch.strategy", "派单策略", "NEAREST",
                List.of(opt("最近可用", "NEAREST"), opt("负载均衡", "BALANCED"), opt("评分优先", "SCORE"), opt("区域分包", "AREA")),
                "智能调度的派单规则");
        number("DISPATCH", "dms.dispatch.weight.distance", "权重·距离", "1", 0, 10, null, "距离越近越优先（0=不参与）");
        number("DISPATCH", "dms.dispatch.weight.load", "权重·负载", "1", 0, 10, null, "在途单越少越优先");
        number("DISPATCH", "dms.dispatch.weight.score", "权重·评分", "1", 0, 10, null, "综合评分越高越优先");
        number("DISPATCH", "dms.dispatch.max.concurrent", "单人在途上限", "5", 0, 100, "单", "0=不限；达到上限不再派单");
        bool("DISPATCH", "dms.dispatch.require.online", "仅向空闲配送员派单", "true", "关闭后休息中之外均可参与");
        number("DISPATCH", "dms.dispatch.max.load.kg", "单任务载重上限", "2000", 0, 100000, "kg", "0=不限；超限不可指派");
        number("DISPATCH", "dms.dispatch.max.volume.m3", "单任务容积上限", "10", 0, 1000, "m³", "0=不限");
        number("DISPATCH", "dms.dispatch.timeout.escalate.minutes", "超时升级阈值", "30", 0, 1440, "分钟", "在途任务超此值进入超时统计");
        bool("DISPATCH", "dms.dispatch.area.strict", "区域分包严格模式", "false",
                "仅 AREA 策略生效：开启后任务线路未绑定该配送员即不可派，关闭则绑定者优先");

        // ── 配送跟踪（跟踪/实时跟踪读取）──
        number("TRACKING", "dms.tracking.online.minutes", "在线判定阈值", "2", 1, 1440, "分钟", "最近上报在 N 分钟内视为在线");
        number("TRACKING", "dms.tracking.speed.limit", "超速阈值", "60", 0, 300, "km/h", "超过即产生超速预警");
        number("TRACKING", "dms.tracking.stop.minutes", "异常停留阈值", "15", 1, 1440, "分钟", "相邻点位移<50 米且间隔超此值");
        number("TRACKING", "dms.tracking.deviation.meters", "偏航阈值", "1000", 0, 100000, "米", "偏离配送基线上限");
        timeRange("TRACKING", "dms.tracking.collect.hours", "位置采集时段", "00:00-23:59", "合规：仅该时段采集，支持跨天");
        number("TRACKING", "dms.tracking.retention.days", "轨迹保留天数", "0", 0, 3650, "天", "0=不自动清理");

        // ── 签收 ──
        number("SIGN", "dms.sign.deviation.threshold", "签收偏差阈值", "100", 0, 100000, "米", "超出仅标记待复核，不拒绝签收");

        // ── 结算与收款 ──
        number("SETTLEMENT", "dms.settlement.base.fee", "起步价", "5", 0, 100000, "元", "配送结算起步价");
        number("SETTLEMENT", "dms.settlement.free.distance.km", "免费距离", "0", 0, 1000, "km", "超出后按单价计费");
        number("SETTLEMENT", "dms.settlement.per.km.rate", "每公里单价", "2", 0, 100, "元/km", "超距单价");
        number("SETTLEMENT", "dms.settlement.time.surcharge.rate", "时段加价率", "1.5", 0, 100, "倍", "高峰时段系数");
        number("SETTLEMENT", "dms.settlement.urgent.surcharge", "加急附加费", "10", 0, 100000, "元", "加急单附加");
        text("SETTLEMENT", "dms.payment.qrcode.base-url", "收款二维码基址", "", false,
                "未配置时不生成二维码（收款管理降级为线下登记）");
        number("SETTLEMENT", "dms.payment.cash.limit.per.order", "单笔现金收款限额", "0", 0, 1000000, "元",
                "0=不限；超限拒绝现金收款，引导改用扫码/POS");
        number("SETTLEMENT", "dms.payment.cash.limit.daily", "单日现金收款限额", "0", 0, 10000000, "元",
                "0=不限；按配送员当日现金累计校验");
        number("SETTLEMENT", "dms.payment.handover.deadline.hours", "交款时限", "24", 0, 720, "小时",
                "自收款起算，超时进入待上交预警；0=不限");

        // ── 实名与核验 ──
        number("VERIFICATION", "verification.position.threshold.meters", "人车位置核验阈值", "500", 0, 100000, "米", "超过判定人车分离");
        number("VERIFICATION", "verification.binding.max.hours", "绑定最长时长", "12", 0, 168, "小时", "超出提示交车");
        number("VERIFICATION", "verification.stay.threshold.minutes", "异常滞留阈值", "60", 1, 1440, "分钟", "超时提示滞留");
        number("VERIFICATION", "verification.separation.consecutive", "人车分离连续次数", "3", 1, 100, "次", "连续超阈值告警");
        bool("VERIFICATION", "verification.eligibility.enforce", "强制准入校验", "false", "开启后未通过准入的配送员不可接单");
        bool("VERIFICATION", "verification.departure.inspection.required", "出车必检", "true", "出车须完成车辆巡检");
        bool("VERIFICATION", "verification.return.inspection.required", "回车必检", "true", "回车须完成车辆巡检");
        number("VERIFICATION", "kyc.cert.expire.warn.days", "证照到期提醒天数", "30", 0, 365, "天", "到期前 N 天预警");

        // ── 能耗与补能 ──
        number("ENERGY", "energy.consumption.max.per100km", "百公里能耗上限", "30", 0, 1000, null, "超出标记异常补能");
        number("ENERGY", "energy.unit.cost.max", "单位能耗成本上限", "5", 0, 100, "元", "超出标记异常补能");
        timeRange("ENERGY", "energy.workhours", "补能作业时段", "06:00-23:00", "仅该时段可登记补能");

        // ── 渠道对接（《渠道管理》派单/回调/凭据加密读取）──
        number("CHANNEL", "dms.channel.push.retry", "下单重试次数", "2", 0, 5, "次",
                "向外部平台下单失败后的重试次数（指数退避，0=不重试）");
        bool("CHANNEL", "dms.channel.push.fallback", "渠道失败降级自有运力", "true",
                "外部渠道不可用时自动回退自有运力派单，避免派单中断");
        number("CHANNEL", "dms.channel.callback.tolerance.seconds", "回调时间戳容差", "300", 0, 3600, "秒",
                "超出容差的回调视为过期请求并拒绝（防重放）");
        bool("CHANNEL", "dms.channel.config.encrypt", "凭据加密存储", "true",
                "config_json 中 AppSecret/token 等敏感值加密落库（出参仍脱敏）");
        text("CHANNEL", "dms.channel.config.encrypt-key", "凭据加密密钥", "", true,
                "留空=使用内置默认密钥（仅开发/演示，生产必须显式配置）");

        // ── 地图与位置服务（敏感键：脱敏展示）──
        enumOf("MAP", "map.default-provider", "默认地图服务商", "amap",
                List.of(opt("高德", "amap"), opt("腾讯", "tencent"), opt("百度", "baidu")),
                "地理编码/逆编码/路径规划默认走该服务商");
        text("MAP", "map.amap.api-key", "高德 Web API Key", "", true, "服务端调用高德接口（脱敏展示，留空=不修改）");
        text("MAP", "map.amap.js-key", "高德 JS SDK Key", "", true, "前端底图（无 Key 自动回退矢量画布）");
        text("MAP", "map.tencent.api-key", "腾讯地图 API Key", "", true, "服务端调用腾讯接口");
        text("MAP", "map.baidu.api-key", "百度地图 API Key", "", true, "服务端调用百度接口");

        // ── API监控（《API监控开发文档》§3.5.3 告警阈值 + §合规留存）──
        number("MONITOR", "monitor.threshold.error-rate", "错误率告警线", "5", 0, 100, "%",
                "当日错误率高于该值产生告警（默认 5）");
        number("MONITOR", "monitor.threshold.p95-ms", "P95 耗时告警线", "2000", 0, 600000, "ms",
                "当日 P95 高于该值产生告警（默认 2000）");
        number("MONITOR", "monitor.threshold.fail-count", "失败次数告警线", "20", 0, 1000000, "次",
                "当日失败次数高于该值产生告警（默认 20）");
        number("MONITOR", "monitor.threshold.sync-fail-count", "库存同步失败告警线", "0", 0, 1000000, "条",
                "库存同步失败数高于该值产生告警（0=有失败即告警）");
        number("MONITOR", "monitor.threshold.silence-minutes", "告警静默期", "30", 0, 1440, "分钟",
                "同类型告警在静默期内只外发一次事件（0=不静默）");
        number("MONITOR", "monitor.threshold.auto-refresh-seconds", "页面自动刷新间隔", "30", 0, 3600, "秒",
                "API监控页自动刷新间隔（0=不自动刷新）");
        number("MONITOR", "monitor.log.retention-days", "调用日志保留天数", "30", 0, 3650, "天",
                "按日清理 api_access_log（0=不自动清理）");
    }

    private DmsConfigMetaRegistry() {
    }

    /** 按键取元数据（未登记返回 empty，调用方按「其他」分组处理） */
    public static Optional<DmsConfigMeta> find(String key) {
        return key == null ? Optional.empty() : Optional.ofNullable(REGISTRY.get(key));
    }

    /** 全部已登记元数据（按注册顺序） */
    public static List<DmsConfigMeta> all() {
        return new ArrayList<>(REGISTRY.values());
    }

    /** 未登记键的兜底元数据：归「其他」组、按 TEXT 处理、可改、热生效 */
    public static DmsConfigMeta fallback(String key) {
        DmsConfigMeta meta = base("OTHER", key, key, ValueType.TEXT);
        meta.setDefaultValue(null);
        meta.setEditable(true);
        meta.setEffect("HOT");
        meta.setSecret(false);
        meta.setDesc("未登记的配置键（请在 DmsConfigMetaRegistry 中补充元数据）");
        return meta;
    }

    // ── 注册辅助 ──

    private static void number(String group, String key, String name, String defaultValue,
                               Integer min, Integer max, String unit, String desc) {
        DmsConfigMeta meta = base(group, key, name, ValueType.NUMBER);
        meta.setDefaultValue(defaultValue);
        meta.setMin(min == null ? null : BigDecimal.valueOf(min));
        meta.setMax(max == null ? null : BigDecimal.valueOf(max));
        meta.setUnit(unit);
        meta.setDesc(desc);
    }

    private static void bool(String group, String key, String name, String defaultValue, String desc) {
        DmsConfigMeta meta = base(group, key, name, ValueType.BOOLEAN);
        meta.setDefaultValue(defaultValue);
        meta.setDesc(desc);
    }

    private static void text(String group, String key, String name, String defaultValue, boolean secret, String desc) {
        DmsConfigMeta meta = base(group, key, name, ValueType.TEXT);
        meta.setDefaultValue(defaultValue);
        meta.setSecret(secret);
        meta.setDesc(desc);
    }

    private static void timeRange(String group, String key, String name, String defaultValue, String desc) {
        DmsConfigMeta meta = base(group, key, name, ValueType.TIME_RANGE);
        meta.setDefaultValue(defaultValue);
        meta.setDesc(desc);
    }

    private static void enumOf(String group, String key, String name, String defaultValue,
                               List<EnumOption> options, String desc) {
        DmsConfigMeta meta = base(group, key, name, ValueType.ENUM);
        meta.setDefaultValue(defaultValue);
        meta.setOptions(options);
        meta.setDesc(desc);
    }

    private static EnumOption opt(String label, String value) {
        EnumOption option = new EnumOption();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }

    /** 统一默认：可改 + 热生效 + 非敏感 */
    private static DmsConfigMeta base(String group, String key, String name, ValueType type) {
        DmsConfigMeta meta = new DmsConfigMeta();
        meta.setGroup(group);
        meta.setGroupText(GROUPS.getOrDefault(group, group));
        meta.setConfigKey(key);
        meta.setName(name);
        meta.setValueType(type);
        meta.setEditable(true);
        meta.setEffect("HOT");
        meta.setSecret(false);
        REGISTRY.put(key, meta);
        return meta;
    }
}
