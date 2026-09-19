package cn.aiedge.audit.controller;

import cn.aiedge.audit.vo.SysLoginLogVO;
import cn.aiedge.audit.vo.SysOperLogVO;
import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysLoginLog;
import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysLoginLogMapper;
import cn.aiedge.base.mapper.SysOperLogMapper;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 操作日志 / 登录日志 Controller —— 「设置 → 账套操作 → 操作日志」（菜单 80630）的唯一在用后端
 *
 * <h3>双实现收敛裁定（2026-09-18，依据《操作日志开发文档》§6.4 / §9.2-P1）</h3>
 * 本仓库存在两套日志接口实现：
 * <ul>
 *   <li><b>本控制器</b>（前缀 <code>/api/log</code>）：页面 {@code views/set/operation-log/index.vue}
 *       与前端封装 {@code api/log.ts} 全部指向它 → <b>唯一在用实现，保留</b>；</li>
 *   <li><b>平行实现</b>：{@code cn.aiedge.base.controller.LogManageController}
 *       （前缀 <code>/api/system/log</code>，13 个端点，权限注解齐全）——
 *       <b>全仓库无任何页面 / api 封装引用</b>（前端源码 grep <code>/system/log</code> 命中 0）。</li>
 * </ul>
 * 裁定：<b>保留本控制器为唯一在用实现</b>，不删除 {@code LogManageController}（避免误伤潜在的
 * 非前端调用方，且其端点能力被本控制器覆盖后可用于后续处置）。裁定理由：① 前端只有一套封装，
 * 切到平行实现需重接前端且能力不增；② 文档 §9.2 的首选建议即「保留本实现 + 补齐权限注解 + 补权限种子」。
 * <p>本轮据此把本控制器缺失的三件事全部补齐：<b>权限注解</b>（沿用
 * {@code LogManageController} 已在用的 {@code log:oper:*} / {@code log:login:*} 权限码，
 * <b>不新造权限码</b>）、<b>租户口径</b>（不再硬编码 {@code tenant_id = 1}）、
 * <b>「清空」语义</b>（改成显式的「按保留天数清理」，并提供删除条数预览）。
 *
 * <h3>租户口径</h3>
 * <ul>
 *   <li>{@code sys_oper_log}：<b>有</b> tenant_id 列且不在 {@code IGNORE_TENANT_TABLES} 清单内 →
 *       由 MyBatis-Plus 租户拦截器按<b>当前登录会话</b>自动注入，故此处不再手写租户条件；</li>
 *   <li>{@code sys_login_log}：在 {@code IGNORE_TENANT_TABLES} 清单内（登录发生在认证之前，
 *       写入链路必须跨租户可查），自动注入被关闭 → 此处按当前会话租户<b>显式</b>过滤；</li>
 *   <li>两种情况在「平台超管（会话整体豁免）」下都收敛为<b>全局视野</b>，与
 *       {@code AiReadyTenantLineInnerInterceptor#shouldSkip()} 的口径完全一致。</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "操作日志", description = "系统日志 / 登录日志的查询、详情、清理与导出")
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
@SaCheckLogin
public class SysLogStubController {

    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysUserMapper sysUserMapper;

    /** 允许的日期入参格式（同时兼容「仅日期」与「日期+时间」两种写法） */
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出文件名时间戳 */
    private static final DateTimeFormatter FILE_TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** 「清理历史日志」的最短保留天数保护：小于该值一律拒绝，避免误删近期日志 */
    private static final int MIN_RETAIN_DAYS = 7;

    /** 单次导出最大条数（防止一次性拉爆内存） */
    private static final int MAX_EXPORT_ROWS = 10000;

    // ═══════════════════════════ 系统日志（sys_oper_log） ═══════════════════════════

    @Operation(summary = "系统日志分页查询")
    @GetMapping("/page")
    @SaCheckPermission("log:oper:list")
    public Result<Page<SysOperLogVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<SysOperLog> wrapper = buildOperWrapper(
                module, operationType, operatorName, startDate, endDate, status);
        Page<SysOperLog> result = operLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(toOperPage(result));
    }

    @Operation(summary = "系统日志详情（含请求参数 / 响应结果，列表接口不返回这两个大字段）")
    @GetMapping("/{id}")
    @SaCheckPermission("log:oper:detail")
    public Result<SysOperLogVO> getById(@PathVariable Long id) {
        SysOperLog log = operLogMapper.selectById(id);
        if (log == null) {
            return Result.fail("日志不存在");
        }
        SysOperLogVO vo = toOperVo(log);
        // 详情接口才回填两个 text 大字段（列表侧裁剪，避免拖大字段）
        vo.setRequestParams(log.getRequestParams());
        vo.setResponseResult(log.getResponseResult());
        // 详情只有单条，单独补姓名
        Map<Long, String> nameMap = loadRealNames(Collections.singletonList(log.getUserId()));
        vo.setRealName(nameMap.get(log.getUserId()));
        return Result.ok(vo);
    }

    @Operation(summary = "系统日志的模块下拉值域（数据驱动：对 module 去重）")
    @GetMapping("/modules")
    @SaCheckPermission("log:oper:list")
    public Result<List<String>> modules() {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(SysOperLog::getModule)
                .ne(SysOperLog::getModule, "")
                .select(SysOperLog::getModule)
                .groupBy(SysOperLog::getModule)
                .orderByAsc(SysOperLog::getModule);

        List<String> modules = operLogMapper.selectList(wrapper)
                .stream()
                .map(SysOperLog::getModule)
                .collect(Collectors.toList());
        return Result.ok(modules);
    }

    @Operation(summary = "系统日志的操作类型下拉值域（数据驱动：对 action 去重）")
    @GetMapping("/operation-types")
    @SaCheckPermission("log:oper:list")
    public Result<List<String>> operationTypes() {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(SysOperLog::getAction)
                .ne(SysOperLog::getAction, "")
                .select(SysOperLog::getAction)
                .groupBy(SysOperLog::getAction)
                .orderByAsc(SysOperLog::getAction);

        List<String> types = operLogMapper.selectList(wrapper)
                .stream()
                .map(SysOperLog::getAction)
                .collect(Collectors.toList());
        return Result.ok(types);
    }

    @Operation(summary = "清理前预览：返回「保留 days 天」将删除的日志条数（供前端如实展示）")
    @GetMapping("/clear/preview")
    @SaCheckPermission("log:oper:delete")
    public Result<Integer> clearPreview(@RequestParam(defaultValue = "90") int days) {
        if (days < MIN_RETAIN_DAYS) {
            return Result.fail("日志保留天数不能少于" + MIN_RETAIN_DAYS + "天");
        }
        Long count = operLogMapper.selectCount(retainCleanupWrapper(days));
        return Result.ok(count == null ? 0 : count.intValue());
    }

    /**
     * 按保留天数清理历史日志。
     *
     * <p><b>语义订正（2026-09-18）</b>：本端点只删除 <b>{@code oper_time < now() - days}</b> 的记录，
     * <b>不是「清空所有日志」</b>。原前端文案写「确定要清空所有操作日志吗」与实际行为不符（P0），
     * 现由前端改为如实文案并显式展示将删除的条数（见 {@link #clearPreview(int)}）。
     *
     * <p>⚠️ 平台超管（会话豁免租户隔离）调用时会跨租户删除，属平台级语义；
     * 普通租户会话只删本租户数据。
     */
    @Operation(summary = "清理历史日志（只删 oper_time 早于「保留天数」的记录，非清空全部）")
    @DeleteMapping("/clear")
    @SaCheckPermission("log:oper:delete")
    public Result<Integer> clearLogs(
            @RequestParam(defaultValue = "90") int days) {
        if (days < MIN_RETAIN_DAYS) {
            return Result.fail("日志保留天数不能少于" + MIN_RETAIN_DAYS + "天");
        }
        int deleted = operLogMapper.delete(retainCleanupWrapper(days));
        return Result.ok(deleted);
    }

    @Operation(summary = "导出系统日志（CSV，带 UTF-8 BOM）")
    @GetMapping("/export")
    @SaCheckPermission("log:oper:export")
    public void exportLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws Exception {

        LambdaQueryWrapper<SysOperLog> wrapper = buildOperWrapper(
                module, operationType, operatorName, startDate, endDate, null);

        Page<SysOperLog> result = operLogMapper.selectPage(new Page<>(1, MAX_EXPORT_ROWS), wrapper);
        List<SysOperLog> logs = result.getRecords();
        Map<Long, String> nameMap = loadRealNames(
                logs.stream().map(SysOperLog::getUserId).collect(Collectors.toSet()));

        StringBuilder sb = new StringBuilder();
        // 表头与台账口径一致；不导出 请求参数 / 响应结果（可能含密码、token 等敏感数据，见 OWASP 日志准则）
        sb.append("日志ID,操作时间,操作员,姓名,模块,操作类型,请求URL,请求方式,IP地址,耗时(ms),状态,错误信息\n");
        for (SysOperLog log : logs) {
            sb.append(csv(log.getId())).append(',');
            sb.append(csvDateTime(log.getOperTime())).append(',');
            sb.append(csv(log.getUsername())).append(',');
            sb.append(csv(nameMap.get(log.getUserId()))).append(',');
            sb.append(csv(log.getModule())).append(',');
            sb.append(csv(log.getAction())).append(',');
            sb.append(csv(log.getRequestUrl())).append(',');
            sb.append(csv(log.getRequestMethod())).append(',');
            sb.append(csv(log.getOperIp())).append(',');
            sb.append(csv(log.getCostTime())).append(',');
            sb.append(csv(log.getStatus() != null && log.getStatus() == 0 ? "成功" : "失败")).append(',');
            sb.append(csv(log.getErrorMsg())).append('\n');
        }
        writeCsv(response, "oper_log_" + LocalDateTime.now().format(FILE_TS_FMT) + ".csv", sb.toString());
    }

    // ═══════════════════════════ 登录日志（sys_login_log） ═══════════════════════════

    @Operation(summary = "登录日志分页查询（对标 ql361「登录日志」Tab）")
    @GetMapping("/login/page")
    @SaCheckPermission("log:login:list")
    public Result<Page<SysLoginLogVO>> loginPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer loginType,
            @RequestParam(required = false) Integer loginResult,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LambdaQueryWrapper<SysLoginLog> wrapper = buildLoginWrapper(
                username, loginType, loginResult, startDate, endDate);
        Page<SysLoginLog> result = loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(toLoginPage(result));
    }

    @Operation(summary = "导出登录日志（CSV，带 UTF-8 BOM）")
    @GetMapping("/login/export")
    @SaCheckPermission("log:login:export")
    public void exportLoginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer loginType,
            @RequestParam(required = false) Integer loginResult,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws Exception {

        LambdaQueryWrapper<SysLoginLog> wrapper = buildLoginWrapper(
                username, loginType, loginResult, startDate, endDate);
        Page<SysLoginLog> result = loginLogMapper.selectPage(new Page<>(1, MAX_EXPORT_ROWS), wrapper);
        List<SysLoginLog> logs = result.getRecords();
        Map<Long, String> nameMap = loadRealNames(
                logs.stream().map(SysLoginLog::getUserId).collect(Collectors.toSet()));

        StringBuilder sb = new StringBuilder();
        sb.append("日志ID,登录时间,操作员,姓名,登录类型,登录结果,失败原因,登录IP,登录地点,浏览器,操作系统,设备类型,退出时间\n");
        for (SysLoginLog log : logs) {
            sb.append(csv(log.getId())).append(',');
            sb.append(csvDateTime(log.getLoginTime())).append(',');
            sb.append(csv(log.getUsername())).append(',');
            sb.append(csv(nameMap.get(log.getUserId()))).append(',');
            sb.append(csv(loginTypeText(log.getLoginType()))).append(',');
            sb.append(csv(log.getLoginResult() != null && log.getLoginResult() == 0 ? "成功" : "失败")).append(',');
            sb.append(csv(log.getFailReason())).append(',');
            sb.append(csv(log.getLoginIp())).append(',');
            sb.append(csv(log.getLoginLocation())).append(',');
            sb.append(csv(log.getBrowser())).append(',');
            sb.append(csv(log.getOs())).append(',');
            sb.append(csv(log.getDeviceType())).append(',');
            sb.append(csvDateTime(log.getLogoutTime())).append('\n');
        }
        writeCsv(response, "login_log_" + LocalDateTime.now().format(FILE_TS_FMT) + ".csv", sb.toString());
    }

    // ═══════════════════════════ 查询条件构造 ═══════════════════════════

    /**
     * 系统日志查询条件。
     * ⚠️ 不写 tenant_id 条件：{@code sys_oper_log} 由租户拦截器按当前会话自动注入。
     */
    private LambdaQueryWrapper<SysOperLog> buildOperWrapper(String module, String operationType,
                                                           String operatorName, String startDate,
                                                           String endDate, Integer status) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (hasText(module)) {
            wrapper.eq(SysOperLog::getModule, module);
        }
        if (hasText(operationType)) {
            // 前端「操作类型」对应实体字段 action
            wrapper.eq(SysOperLog::getAction, operationType);
        }
        if (hasText(operatorName)) {
            wrapper.like(SysOperLog::getUsername, operatorName);
        }
        if (status != null) {
            wrapper.eq(SysOperLog::getStatus, status);
        }
        LocalDateTime start = parseDateTime(startDate, false);
        LocalDateTime end = parseDateTime(endDate, true);
        if (start != null) {
            wrapper.ge(SysOperLog::getOperTime, start);
        }
        if (end != null) {
            wrapper.le(SysOperLog::getOperTime, end);
        }
        wrapper.orderByDesc(SysOperLog::getOperTime);
        return wrapper;
    }

    /**
     * 登录日志查询条件。
     * ⚠️ {@code sys_login_log} 在 {@code IGNORE_TENANT_TABLES} 清单内（登录链路需跨租户查账号），
     * 租户拦截器不会自动注入 → 此处显式按当前会话租户过滤。
     */
    private LambdaQueryWrapper<SysLoginLog> buildLoginWrapper(String username, Integer loginType,
                                                             Integer loginResult, String startDate,
                                                             String endDate) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        Long tenantId = currentTenantId();
        if (tenantId != null) {
            wrapper.eq(SysLoginLog::getTenantId, tenantId);
        }
        if (hasText(username)) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        if (loginType != null) {
            wrapper.eq(SysLoginLog::getLoginType, loginType);
        }
        if (loginResult != null) {
            wrapper.eq(SysLoginLog::getLoginResult, loginResult);
        }
        LocalDateTime start = parseDateTime(startDate, false);
        LocalDateTime end = parseDateTime(endDate, true);
        if (start != null) {
            wrapper.ge(SysLoginLog::getLoginTime, start);
        }
        if (end != null) {
            wrapper.le(SysLoginLog::getLoginTime, end);
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        return wrapper;
    }

    /** 「保留 days 天」的清理条件：只针对早于阈值的记录（租户条件由拦截器注入） */
    private LambdaQueryWrapper<SysOperLog> retainCleanupWrapper(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysOperLog::getOperTime, threshold);
        return wrapper;
    }

    /**
     * 当前会话租户 ID；平台超管（会话整体豁免）返回 null 表示「不加租户条件」。
     * 口径与 {@code AiReadyTenantLineInnerInterceptor#shouldSkip()} 完全一致，
     * 避免「oper_log 全局可见、login_log 却只看租户 1」的口径分裂。
     */
    private Long currentTenantId() {
        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            return null;
        }
        return MyBatisPlusConfig.getCurrentTenantIdValue();
    }

    // ═══════════════════════════ 实体 → VO ═══════════════════════════

    private Page<SysOperLogVO> toOperPage(Page<SysOperLog> source) {
        List<SysOperLog> records = source.getRecords();
        Map<Long, String> nameMap = loadRealNames(
                records.stream().map(SysOperLog::getUserId).collect(Collectors.toSet()));
        List<SysOperLogVO> vos = records.stream().map(log -> {
            SysOperLogVO vo = toOperVo(log);
            vo.setRealName(nameMap.get(log.getUserId()));
            return vo;
        }).collect(Collectors.toList());

        Page<SysOperLogVO> page = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        page.setRecords(vos);
        return page;
    }

    private Page<SysLoginLogVO> toLoginPage(Page<SysLoginLog> source) {
        List<SysLoginLog> records = source.getRecords();
        Map<Long, String> nameMap = loadRealNames(
                records.stream().map(SysLoginLog::getUserId).collect(Collectors.toSet()));
        List<SysLoginLogVO> vos = records.stream().map(log -> new SysLoginLogVO()
                .setId(log.getId())
                .setUserId(log.getUserId())
                .setUsername(log.getUsername())
                .setRealName(nameMap.get(log.getUserId()))
                .setLoginTime(log.getLoginTime())
                .setLoginType(log.getLoginType())
                .setLoginResult(log.getLoginResult())
                .setFailReason(log.getFailReason())
                .setLoginIp(log.getLoginIp())
                .setLoginLocation(log.getLoginLocation())
                .setBrowser(log.getBrowser())
                .setOs(log.getOs())
                .setDeviceType(log.getDeviceType())
                .setLogoutTime(log.getLogoutTime())
                .setRemark(log.getRemark())
        ).collect(Collectors.toList());

        Page<SysLoginLogVO> page = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        page.setRecords(vos);
        return page;
    }

    /** 单条系统日志 → VO（故意不填 requestParams / responseResult，列表侧裁剪大字段） */
    private SysOperLogVO toOperVo(SysOperLog log) {
        return new SysOperLogVO()
                .setId(log.getId())
                .setUserId(log.getUserId())
                .setUsername(log.getUsername())
                .setModule(log.getModule())
                .setAction(log.getAction())
                .setOperTime(log.getOperTime())
                .setOperIp(log.getOperIp())
                .setOperLocation(log.getOperLocation())
                .setCostTime(log.getCostTime())
                .setStatus(log.getStatus())
                .setErrorMsg(log.getErrorMsg())
                .setRequestMethod(log.getRequestMethod())
                .setRequestUrl(log.getRequestUrl())
                .setMethod(log.getMethod())
                .setDiffData(log.getDiffData());
    }

    /**
     * 批量补齐「姓名」列：sys_oper_log / sys_login_log 只落账号，真实姓名在 sys_user。
     * 取 real_name，为空回退 nickname；两侧都为空则保持 null（前端显示 '-'）。
     * 姓名属展示增强，查询失败不得影响主列表 → 异常兜底为空 Map。
     */
    private Map<Long, String> loadRealNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getRealName)
                    .in(SysUser::getId, ids);
            return sysUserMapper.selectList(wrapper).stream()
                    .collect(Collectors.toMap(
                            SysUser::getId,
                            u -> firstNonBlank(u.getRealName(), u.getNickname()),
                            (a, b) -> a));
        } catch (Exception e) {
            log.warn("补齐操作员姓名失败（不影响列表）: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ═══════════════════════════ 辅助方法 ═══════════════════════════

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    /**
     * 宽松解析日期入参：同时兼容 {@code yyyy-MM-dd} 与 {@code yyyy-MM-dd HH:mm:ss}。
     * 起始日只给日期时按当天 00:00:00，截止日只给日期时按当天 23:59:59
     * （原实现用 {@code @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")} 接收仅日期串会直接 400，
     * 而前端一直是发 {@code YYYY-MM-DD}，即日期筛选此前实际不可用；此处一并修掉）。
     *
     * @param endOfDay 仅日期时是否取当天末尾
     */
    private static LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (!hasText(value)) {
            return null;
        }
        String v = value.trim();
        try {
            if (v.length() <= 10) {
                LocalDate date = LocalDate.parse(v, DATE_ONLY_FMT);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(v.replace('T', ' '), DATE_TIME_FMT);
        } catch (Exception e) {
            log.warn("日志查询日期参数解析失败，已忽略该条件: {}", value);
            return null;
        }
    }

    private static String loginTypeText(Integer loginType) {
        if (loginType == null) {
            return "未知";
        }
        return switch (loginType) {
            case 1 -> "账号密码登录";
            case 2 -> "短信验证码登录";
            case 3 -> "第三方登录";
            default -> "未知";
        };
    }

    /** CSV 时间列：统一格式化为 yyyy-MM-dd HH:mm:ss（避免 ISO 的 T 分隔符） */
    private static String csvDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FMT);
    }

    /** CSV 字段转义：包含逗号、双引号或换行符时用双引号包裹 */
    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /** 写 CSV 响应：UTF-8 + BOM（Excel 正确识别中文）、附件文件名含时间戳 */
    private static void writeCsv(HttpServletResponse response, String filename, String body) throws Exception {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        OutputStream out = response.getOutputStream();
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        out.write(body.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
