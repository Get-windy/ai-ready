package cn.aiedge.tenant.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysMenu;
import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysTenantModule;
import cn.aiedge.base.mapper.SysMenuMapper;
import cn.aiedge.base.mapper.SysTenantModuleMapper;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.vo.Result;
import cn.aiedge.module.mapper.SysModuleMapper;
import cn.aiedge.module.model.SysModule;
import cn.aiedge.tenant.mapper.SysTenantQuotaMapper;
import cn.aiedge.tenant.model.SysTenantQuota;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 应用中心（设置 → 系统配置 → 应用中心，菜单 80625 / {@code set:app-center}）控制器 —— <b>租户级</b>。
 *
 * <p>与系统模块的平台级「模块管理」（{@code ModuleController}，{@code /api/module}，
 * 权限码 {@code system:module:*}）是两件事：本控制器只回答「<b>我这个租户</b>安装了哪些模块、
 * 开通了哪些模块、到期了没有、配额用了多少」，<b>不提供任何写入口</b>
 * （无开通 / 停用 / 续费 / 购买，这些属平台侧授权下发）。</p>
 *
 * <p><b>为什么不加 {@code @SaCheckPermission}？</b>
 * 实测 `sys_permission`（264 行）中 {@code set:%} 与 {@code module:%} 前缀均为 <b>0 行</b>，
 * 而本页原实现依赖的两个码 {@code system:tenant:query} / {@code platform:tenant-package:list}
 * 也<b>都不存在</b>（仅超管走 {@code *} 通配能过）→ 注解齐全但无码等于「非超管全 403」。
 * 因此沿用同模块既有裁定（{@code SetMenuConfigController} 头部注释、《设置模块 README》§5.5）：
 * <b>租户读自己的数据只做登录校验，租户 id 一律取自登录会话</b>，不接受前端传入，
 * 杜绝跨租户读取；不凭空新增权限码（新增后不授予任何角色，只会复现同一个 403 症状）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "应用中心（租户级）", description = "租户查看本租户已安装/已开通的功能模块、到期与配额")
@RestController
@RequestMapping("/api/set/app-center")
@SaCheckLogin
@RequiredArgsConstructor
public class SetAppCenterController {

    private final TenantMapper tenantMapper;
    private final SysTenantModuleMapper tenantModuleMapper;
    private final SysModuleMapper sysModuleMapper;
    private final SysTenantQuotaMapper tenantQuotaMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 短信用量是**跨模块只读聚合**（mkt_sms_setting / mkt_sms_record 属营销域）：
     * 走 JdbcTemplate 直读，不引入该域的 Mapper/实体，避免核心模块与业务域耦合。
     */
    private final JdbcTemplate jdbcTemplate;

    /** 租户端菜单 */
    private static final String CLIENT_TYPE_TENANT_ADMIN = "tenant-admin";

    /**
     * 「短信及其他」区的能力入口：**只列本系统真实存在（且启用）的租户端菜单**，
     * 菜单编码取自 {@code sys_menu.menu_code}（均已实核存在）。
     *
     * <p>顺序即页面展示顺序：短信 → 物流查询 → 智能排线 → 存储/图片。
     * 缺失的条目<b>直接不返回</b>（前端据此如实提示缺口），
     * 绝不用第三方厂商商品名（短信包 / 企汇存储空间 / 商米L2 等）造数。
     * 本系统无增值服务商品目录表，故此处只做「已有能力入口」，不做商品展示。</p>
     */
    private static final List<String> CAPABILITY_MENU_CODES = List.of(
            "mkt:sms-send",   // 营销 → 发短信
            "dms:tracking",   // 配送 → 配送跟踪
            "dms:route-plan", // 配送 → 路线规划（对应「智能排线」）
            "md:image"        // 资料 → 图片管理
    );

    /**
     * 概览卡：公司名称 / 到期日期（含剩余天数）/ 配额用量 / 模块计数 / 短信用量。
     *
     * <p>数据源：`sys_tenant`（公司名称·到期日期，本页为租户级只读展示）、
     * `sys_tenant_quota`（用户数·存储·API 调用用量，表存在且有数据，本次正式接入）、
     * `sys_module` + `sys_tenant_module`（模块计数）、
     * `mkt_sms_setting` + `mkt_sms_record`（短信用量，2026-09-18 核实存在后正式接入）。</p>
     */
    @Operation(summary = "应用中心概览（公司名称 / 到期日期 / 配额 / 模块计数 / 短信用量）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long tenantId = requireTenantId();

        Map<String, Object> data = new HashMap<>();

        // ── 租户基本信息（公司名称 / 租户编码 / 等级 / 到期日期）──
        SysTenant tenant = tenantMapper.selectById(tenantId);
        data.put("companyName", tenant == null ? null : tenant.getTenantName());
        data.put("tenantCode", tenant == null ? null : tenant.getTenantCode());
        data.put("tenantLevel", tenant == null ? null : tenant.getLevel());

        LocalDateTime expireTime = tenant == null ? null : tenant.getExpireTime();
        data.put("expireDate", expireTime == null ? null : expireTime.toLocalDate().toString());
        // 剩余天数：无到期日期（永久）时为 null，前端显示「长期有效」
        data.put("expireDays", expireTime == null ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), expireTime.toLocalDate()));

        // ── 配额用量（sys_tenant_quota；无行时各字段为 null，前端显示「未配置」）──
        List<SysTenantQuota> quotas = tenantQuotaMapper.selectList(
                new LambdaQueryWrapper<SysTenantQuota>()
                        .eq(SysTenantQuota::getTenantId, tenantId)
                        .orderByAsc(SysTenantQuota::getId));
        SysTenantQuota quota = quotas.isEmpty() ? null : quotas.get(0);

        Map<String, Object> quotaMap = new HashMap<>();
        quotaMap.put("maxUsers", quota == null ? null : quota.getMaxUsers());
        quotaMap.put("usedUsers", quota == null ? null : quota.getUsedUsers());
        quotaMap.put("maxStorage", quota == null ? null : quota.getMaxStorage());
        quotaMap.put("usedStorage", quota == null ? null : quota.getUsedStorage());
        quotaMap.put("maxApiCalls", quota == null ? null : quota.getMaxApiCalls());
        quotaMap.put("usedApiCalls", quota == null ? null : quota.getUsedApiCalls());
        data.put("quota", quotaMap);

        // ── 模块计数：已安装（注册表） / 已开通（本租户授权记录）──
        List<SysModule> installed = sysModuleMapper.selectInstalledModules();
        data.put("installedModuleCount", installed.size());
        data.put("openedModuleCount", tenantModuleMapper.selectList(
                new LambdaQueryWrapper<SysTenantModule>()
                        .eq(SysTenantModule::getTenantId, tenantId)).size());

        // ── 短信用量（真实计量来源，2026-09-18 核实后接入）──
        data.put("sms", smsUsage(tenantId));
        // ── 物流查询次数：核实后仍无计量来源 → 如实说明缺什么（前端不再只显示含糊的「未接入」）──
        data.put("logisticsQuery", logisticsQueryUsage());

        return Result.ok(data);
    }

    /**
     * 短信用量（**真实表，非估算**）。
     *
     * <p>计量来源已实核（devdb 2026-09-18）：</p>
     * <ul>
     *   <li>{@code mkt_sms_setting}（营销 → 发短信：短信设置，按租户单行）→
     *       {@code quota_total} 总配额、{@code quota_used} 已用；发送侧
     *       {@code SmsSendServiceImpl} 每次发送都 {@code quota_used += sent}，
     *       故「剩余短信 = quota_total - quota_used」是**真实余量**，不是编的数字。</li>
     *   <li>{@code mkt_sms_record}（发送记录台账，一行 = 一条短信）→ 本月已发送条数
     *       （营销发短信与配额累计是两套口径：前者是记录行数，后者是配额计数器，
     *       故两个数字都返回并各自标注来源，不做「取其一冒充全部」）。</li>
     * </ul>
     *
     * <p>无本租户设置行时 {@code available=false}（前端显示「未配置」，不显示 0 余量）。</p>
     *
     * <p>⚠️ 两张表都是**跨模块表**（erp-marketing），此处用 {@link JdbcTemplate} 只读聚合，
     * 不引入该模块的 Mapper/实体：本控制器所在模块不与营销域耦合，且读的是<b>本租户</b>数据，
     * SQL 显式带 {@code tenant_id = ?}（JdbcTemplate 不走租户拦截器，条件必须自己写）。</p>
     */
    private Map<String, Object> smsUsage(Long tenantId) {
        Map<String, Object> sms = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT quota_total, quota_used FROM mkt_sms_setting "
                            + "WHERE tenant_id = ? AND deleted = 0 ORDER BY id LIMIT 1",
                    tenantId);
            if (rows.isEmpty()) {
                sms.put("available", false);
                sms.put("reason", "本租户未配置短信配额（mkt_sms_setting 无本租户行）");
                return sms;
            }
            Integer total = toInteger(rows.get(0).get("quota_total"));
            Integer used = toInteger(rows.get(0).get("quota_used"));
            sms.put("available", true);
            sms.put("quotaTotal", total);
            sms.put("quotaUsed", used);
            // 余量只在总配额确实配置了的时候才算（total 为空 → 余量未知，绝不当作 0）
            sms.put("quotaRemain", (total == null || used == null) ? null : Math.max(total - used, 0));

            Long monthSent = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM mkt_sms_record WHERE tenant_id = ? "
                            + "AND (deleted IS NULL OR deleted = 0) "
                            + "AND create_time >= date_trunc('month', now())",
                    Long.class, tenantId);
            sms.put("monthSent", monthSent);
            sms.put("source", "mkt_sms_setting（配额）+ mkt_sms_record（本月发送记录）");
        } catch (Exception e) {
            // 表缺失 / 结构不符：如实降级为「不可用 + 原因」，绝不让整个概览接口 500
            log.warn("[应用中心] 短信用量读取失败（按不可用降级）：{}", e.getMessage());
            sms.put("available", false);
            sms.put("reason", "短信用量读取失败：" + e.getMessage());
        }
        return sms;
    }

    /**
     * 物流查询次数：**如实说明为什么没有**（核实过程见下），不返回任何数字。
     *
     * <p>2026-09-18 实核 devdb：全库与「物流」相关的表为
     * {@code dms_logistics_ship}（物流发货单）、{@code erp_sale_order_logistics} /
     * {@code erp_purchase_order_logistics}（订单物流信息）、{@code erp_partner_logistics_ext}
     * （合作物流扩展）、{@code dms_channel_callback_log}（渠道回调），
     * <b>没有任何一张记录「对外部运单轨迹的查询次数」</b>；代码侧也未检索到快递轨迹查询的对接实现
     * （无 快递100 / 运单查询 之类的 client 调用）。故本系统确实不具备该计量口径，
     * 保持「未接入」并写清缺什么（不在此处造表、造数）。</p>
     */
    private Map<String, Object> logisticsQueryUsage() {
        Map<String, Object> item = new HashMap<>();
        item.put("available", false);
        item.put("reason", "缺少物流查询计量表，暂无法统计（本系统未对接外部运单轨迹查询，故无查询次数记录；"
                + "现有物流表均为发货单/订单物流信息，不记录查询动作）");
        return item;
    }

    /** PostgreSQL 计数字段可能是 Integer/Long/BigInteger → 统一转 Integer（转不动返回 null） */
    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 功能模块清单 = 系统「模块注册表」(`sys_module`) 左连接本租户开通记录 (`sys_tenant_module`)。
     *
     * <p>这样即使某租户一条开通记录都没有，功能区也**不会是一块空白**：
     * 页面能如实显示「本系统已安装 N 个模块，本租户已开通 M 个」，
     * 而不是像原实现那样「空态 + 无法区分是没开通还是查不到」。</p>
     */
    @Operation(summary = "功能模块清单（已安装 + 本租户是否已开通）")
    @GetMapping("/modules")
    public Result<List<Map<String, Object>>> modules() {
        Long tenantId = requireTenantId();

        List<SysModule> installed = sysModuleMapper.selectInstalledModules();
        List<SysTenantModule> opened = tenantModuleMapper.selectList(
                new LambdaQueryWrapper<SysTenantModule>()
                        .eq(SysTenantModule::getTenantId, tenantId));

        Map<String, SysTenantModule> openedByCode = new HashMap<>();
        for (SysTenantModule item : opened) {
            if (item.getModuleCode() != null) {
                openedByCode.put(item.getModuleCode(), item);
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> handled = new HashSet<>();

        // 1) 注册表模块（按注册表 sort_order 排序）
        for (SysModule module : installed) {
            items.add(buildModuleItem(module.getModuleCode(), module.getModuleName(), module.getVersion(),
                    module.getDescription(), module.getStatus(), module.getSortOrder(),
                    openedByCode.get(module.getModuleCode())));
            handled.add(module.getModuleCode());
        }

        // 2) 注册表里没有、但本租户确有开通记录的编码（如历史细粒度码 sale:order）——如实补齐，不隐藏
        for (SysTenantModule item : opened) {
            if (item.getModuleCode() == null || handled.contains(item.getModuleCode())) {
                continue;
            }
            items.add(buildModuleItem(item.getModuleCode(), item.getModuleName(), null,
                    null, null, null, item));
        }

        return Result.ok(items);
    }

    /**
     * 「短信及其他」区的真实能力入口（本系统已有的租户端菜单）。
     *
     * <p>数据源 `sys_menu`（全局菜单定义，属平台资产，不参与租户隔离）。</p>
     */
    @Operation(summary = "「短信及其他」能力入口（真实存在的租户端菜单）")
    @GetMapping("/capabilities")
    public Result<List<Map<String, Object>>> capabilities() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getClientType, CLIENT_TYPE_TENANT_ADMIN)
                .eq(SysMenu::getStatus, 1)
                .in(SysMenu::getMenuCode, CAPABILITY_MENU_CODES));

        Map<String, SysMenu> byCode = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            byCode.put(menu.getMenuCode(), menu);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        // 按 CAPABILITY_MENU_CODES 的声明顺序返回，保证页面顺序稳定
        for (String code : CAPABILITY_MENU_CODES) {
            SysMenu menu = byCode.get(code);
            if (menu == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("menuCode", menu.getMenuCode());
            item.put("menuName", menu.getMenuName());
            item.put("path", menu.getPath());
            item.put("icon", menu.getIcon());
            items.add(item);
        }
        return Result.ok(items);
    }

    /**
     * 组装「功能模块」卡片数据。
     *
     * @param code        模块编码（注册表口径）
     * @param name        模块名称
     * @param version     注册表版本号（非注册表模块为 null）
     * @param description 注册表描述
     * @param sysStatus   注册表状态：0=禁用 1=启用（null = 不在注册表）
     * @param sortOrder   注册表排序号
     * @param license     本租户的开通记录（null = 未开通）
     */
    private Map<String, Object> buildModuleItem(String code, String name, String version, String description,
                                                Integer sysStatus, Integer sortOrder, SysTenantModule license) {
        Map<String, Object> item = new HashMap<>();
        item.put("moduleCode", code);
        item.put("moduleName", name != null ? name
                : (license == null || license.getModuleName() == null ? code : license.getModuleName()));
        item.put("version", version);
        item.put("description", description);
        item.put("sysStatus", sysStatus);
        item.put("sortOrder", sortOrder);

        boolean licensed = license != null;
        item.put("licensed", licensed);
        item.put("purchaseType", licensed ? license.getPurchaseType() : null);
        LocalDateTime expireTime = licensed ? license.getExpireTime() : null;
        item.put("expireDate", expireTime == null ? null : expireTime.toLocalDate().toString());
        // 开通记录状态：0=正常 1=停用（见表注释；TenantModuleService#getValidModuleCodes 同口径）
        item.put("licenseStatus", licensed ? license.getStatus() : null);
        return item;
    }

    /**
     * 取当前会话租户；拿不到直接抛错（本页所有查询都必须落在明确租户上）。
     */
    private Long requireTenantId() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (tenantId == null) {
            throw new RuntimeException("无法解析当前会话租户，请重新登录后再试");
        }
        return tenantId;
    }
}
