package cn.aiedge.tenant.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.security.PasswordEncryptor;
import cn.aiedge.base.service.SysOperLogService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.tenant.dto.RebuildExecuteRequest;
import cn.aiedge.tenant.rebuild.SystemRebuildService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统重建（设置 → 账套操作 → 系统重建，菜单 70560 / {@code set:rebuild}）控制器 —— <b>危险操作页</b>。
 *
 * <p>对标 ql361「设置 → 账套操作 → 系统重建」实测形态（2026-09-18）：
 * 红色不可恢复警告 + 2 列（选项/描述）的 12 个清除范围复选项 + <b>必输登录密码</b> + 一个「确定」按钮。
 * 本控制器是该页<b>唯一的写路径</b>，也是全系统唯一能「大片清数据」的入口 → 安全要求远高于普通页。</p>
 *
 * <h2>接口</h2>
 * <ul>
 *   <li>{@code GET  /api/set/rebuild/options} —— 12 个范围（键/名称/描述/影响行数预估）+ 警告与提示文案</li>
 *   <li>{@code POST /api/set/rebuild/execute} —— body {@code {options:[...], password:"..."}} → 校验密码后执行</li>
 * </ul>
 *
 * <h2>安全实现要点</h2>
 * <ol>
 *   <li><b>权限码门控</b>：{@code set:rebuild:execute}（本页原实现零权限码，见《系统重建开发文档》§5.5；
 *       种子见迁移 {@code V11.400.0}）。未持码者 403。</li>
 *   <li><b>租户强制取自会话</b>：{@link MyBatisPlusConfig#getCurrentTenantIdValue()}，<b>不接受</b>前端传入
 *       tenantId（伪造无效）→ 跨租户清数据在本接口不可能发生。</li>
 *   <li><b>二次身份验证</b>：每次提交都<b>重新校验登录密码</b>，不复用「已登录」状态、不设免验窗口
 *       （会话被劫持时仍需密码）。校验复用仓库既有 {@link PasswordEncryptor#matches}（BCrypt），
 *       <b>不自写哈希比对</b>。</li>
 *   <li><b>密码不回显、不入日志</b>：响应体不含密码；审计日志写的是「范围 + 逐表影响行数 + 结果」，
 *       <b>刻意不写请求参数</b>（因为请求参数里有明文密码）。</li>
 *   <li><b>真删 + 单事务 + 预检</b>：见 {@link SystemRebuildService}（DELETE 非 setStatus、非 TRUNCATE）。</li>
 * </ol>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "系统重建（危险操作）", description = "按勾选范围清空本租户数据（需登录密码二次验证 + set:rebuild:execute 权限）")
@RestController
@RequestMapping("/api/set/rebuild")
@SaCheckLogin
@RequiredArgsConstructor
public class SetRebuildController {

    private final SystemRebuildService rebuildService;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncryptor passwordEncryptor;
    private final SysOperLogService sysOperLogService;

    /** 审计日志模块名（与其它页面保持一致的「系统重建」） */
    private static final String AUDIT_MODULE = "系统重建";

    /**
     * 12 个清除范围 + 影响行数预估（dry-run，不改任何数据）。
     *
     * <p>清单由后端注册表唯一提供（前端不写死），避免前后端两份清单漂移导致「清错范围」。</p>
     */
    @Operation(summary = "清除范围清单 + 影响行数预估")
    @GetMapping("/options")
    @SaCheckPermission("set:rebuild:execute")
    public ApiResponse<Map<String, Object>> options() {
        Long tenantId = requireTenantId();
        Long userId = SecurityUtils.getCurrentUserId();

        List<String> keys = new ArrayList<>();
        for (SystemRebuildService.Scope s : rebuildService.scopes()) {
            keys.add(s.key());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("options", rebuildService.estimate(tenantId, userId, keys));
        // 警告与提示文案（逐字取自 ql361 实测；由后端下发便于统一口径与 E2E 逐字断言）
        data.put("warning", "系统重建将清除 所有单据、订单 和以下选项数据且 不能恢复！,请谨慎!");
        data.put("tip", "注意： 若需单独备份数据，请联系客服！系统重建后请点击右上角的\"刷新\"或者重新登录后进行系统开账！");
        // 选项间包含关系（对标未显式说明，本系统显式联动；前端按此自动勾选并禁用被包含项）
        data.put("implies", Map.of("warehouse_region", "location", "employee", "operator"));
        data.put("tenantId", String.valueOf(tenantId));
        return ApiResponse.ok(data);
    }

    /**
     * 执行系统重建（二次验证通过后清空勾选范围）。
     *
     * <p>校验顺序：① 至少勾 1 项 → ② 密码非空 → ③ 密码正确 → ④ 目标表预检 → ⑤ 单事务执行。
     * 任一步失败<b>一行数据都不改</b>，也不会留下任何任务记录。</p>
     */
    @Operation(summary = "执行系统重建（需登录密码）")
    @PostMapping("/execute")
    @SaCheckPermission("set:rebuild:execute")
    public ApiResponse<Map<String, Object>> execute(@RequestBody RebuildExecuteRequest request) {
        Long tenantId = requireTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.unauthorized("登录状态已失效，请重新登录后再试");
        }

        // ① 至少勾选 1 项（对标页唯一按钮「确定」的前置校验）
        if (request == null || request.getOptions() == null || request.getOptions().isEmpty()) {
            return ApiResponse.badRequest("请至少选择一项要清除的数据范围");
        }
        // 去重且保持勾选顺序
        List<String> keys = new ArrayList<>(new java.util.LinkedHashSet<>(request.getOptions()));
        // 未知 key 直接拒绝（不静默忽略 —— 静默忽略会让用户以为清了、实际没清）
        for (String key : keys) {
            if (rebuildService.scope(key) == null) {
                return ApiResponse.badRequest("未知的清除范围：" + key);
            }
        }

        // ② 密码非空
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ApiResponse.badRequest("为确保安全，请输入登录密码");
        }

        // ③ 密码校验：复用仓库既有 BCrypt 校验途径（PasswordEncryptor.matches），拒绝后不留任何痕迹
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            // 取不到本人账号（会话租户与账号归属不一致等）→ 拒绝，且不误报成「密码错误」
            return ApiResponse.unauthorized("无法读取当前账号信息，请重新登录后再试");
        }
        if (!passwordMatches(request.getPassword(), user.getPassword())) {
            // 审计：失败也要留痕（写明是密码校验失败；不含密码明文）
            writeAudit(user, tenantId, keys, null, "密码校验失败", 1);
            return ApiResponse.badRequest("登录密码错误，请重新输入");
        }

        // ④ 目标表预检：存在性 + 租户限定链完整（自有 tenant_id 列，或父表有该列且关联列真实存在）
        //    —— 无法在 SQL 上限定租户的表一律整体拒绝，绝不退化成「清全表」
        List<String> bad = rebuildService.preflight(keys);
        if (!bad.isEmpty()) {
            String msg = "以下数据范围不支持清理（目标表不存在，或既无 tenant_id 列、也无法经父表限定租户，"
                    + "为防止跨租户误删已阻断）：" + String.join("、", bad);
            writeAudit(user, tenantId, keys, null, msg, 1);
            return ApiResponse.badRequest(msg);
        }

        // ⑤ 单事务执行；失败整体回滚，并在事务之外补一条失败审计
        List<Map<String, Object>> results;
        try {
            results = rebuildService.execute(tenantId, userId, keys);
        } catch (Exception e) {
            log.error("[系统重建] 租户 {} 执行失败，已整体回滚", tenantId, e);
            writeAudit(user, tenantId, keys, null, "执行失败（已整体回滚）：" + e.getMessage(), 1);
            if (e instanceof BusinessException be) {
                return ApiResponse.badRequest(be.getMessage());
            }
            return ApiResponse.fail("系统重建执行失败，数据已整体回滚：" + e.getMessage());
        }

        writeAudit(user, tenantId, keys, results, null, 0);

        long total = 0;
        for (Map<String, Object> r : results) {
            Object v = r.get("clearedRows");
            total += (v instanceof Number n ? n.longValue() : 0L);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("results", results);
        data.put("totalClearedRows", total);
        data.put("tip", "系统重建已完成。请点击右上角的\"刷新\"或重新登录后进行系统开账！");
        data.put("finishTime", LocalDateTime.now().toString());
        return ApiResponse.ok("重建完成", data);
    }

    /**
     * 密码比对（复用仓库既有 BCrypt 校验途径）。
     *
     * <p>兜底：若库中密码不是 BCrypt 格式（历史遗留 MD5 等），{@code BCrypt.checkpw} 会抛
     * {@code IllegalArgumentException} → 这里统一按「不通过」处理，避免把校验异常变成 500。
     * 任何情况下都<b>不</b>自己写哈希比对。</p>
     */
    private boolean passwordMatches(String raw, String encoded) {
        try {
            return passwordEncryptor.matches(raw, encoded);
        } catch (Exception e) {
            log.warn("[系统重建] 密码校验异常（账号密码可能非 BCrypt 格式）：{}", e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  审计
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 写操作审计（复用现成通道 {@link SysOperLogService#recordLog}，同步落库 —— 同步而非异步，
     * 保证「响应返回时日志已存在」，便于事后追责与 E2E 断言）。
     *
     * <p>⚠️ <b>刻意不写 {@code requestParams}</b>：本接口的请求体含<b>明文登录密码</b>，
     * 任何「保存请求参数」的做法都会把密码写进日志表。因此这里改为写结构化摘要：
     * 范围键 + 逐表影响行数 + 结果 + IP —— 审计信息比原始参数更完整，且不含敏感值。</p>
     */
    private void writeAudit(SysUser user, Long tenantId, List<String> keys,
                            List<Map<String, Object>> results, String errorMsg, int status) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setTenantId(tenantId);
            if (user != null) {
                operLog.setUserId(user.getId());
                operLog.setUsername(user.getUsername());
            } else {
                operLog.setUserId(SecurityUtils.getCurrentUserId());
                operLog.setUsername(SecurityUtils.getCurrentUsername());
            }
            operLog.setModule(AUDIT_MODULE);
            operLog.setAction("DELETE");
            operLog.setMethod("execute");
            operLog.setRequestMethod("POST");
            operLog.setRequestUrl("/api/set/rebuild/execute");
            operLog.setRequestParams(buildAuditSummary(keys, results));
            operLog.setStatus(status);
            operLog.setErrorMsg(errorMsg);
            operLog.setOperTime(LocalDateTime.now());
            operLog.setOperIp(currentIp());
            sysOperLogService.recordLog(operLog);
        } catch (Exception e) {
            // 审计失败绝不阻断业务，但必须在应用日志里留下告警
            log.error("[系统重建] 写审计日志失败", e);
        }
    }

    /** 审计摘要：范围键 + 逐表行数（不含任何请求入参，尤其不含密码） */
    private String buildAuditSummary(List<String> keys, List<Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder("scope=").append(String.join(",", keys));
        if (results != null) {
            for (Map<String, Object> r : results) {
                sb.append("; ").append(r.get("key")).append("=").append(r.get("clearedRows")).append("行");
                Object tb = r.get("tables");
                if (tb instanceof List<?> list) {
                    sb.append("(");
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            sb.append(m.get("table")).append(":").append(m.get("rows")).append(" ");
                        }
                    }
                    sb.append(")");
                }
            }
        }
        return sb.toString();
    }

    private String currentIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /** 租户一律取会话；取不到直接拒绝（宁可不执行，也不猜租户） */
    private Long requireTenantId() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (tenantId == null) {
            throw new RuntimeException("无法解析当前会话租户，请重新登录后再试");
        }
        return tenantId;
    }
}
