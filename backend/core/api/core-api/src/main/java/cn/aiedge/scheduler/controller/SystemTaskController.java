package cn.aiedge.scheduler.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.utils.Result;
import cn.aiedge.scheduler.mapper.ScheduledTaskLogMapper;
import cn.aiedge.scheduler.model.ScheduledTaskLog;
import cn.aiedge.scheduler.model.SystemTaskVO;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 系统任务（设置 → 账套操作 → 系统任务，菜单 70561 / {@code set:system-task}）—— 异步任务执行台账。
 *
 * <p><b>页面性质（本轮已纠正的模型错位）</b>：对标 ql361 是「**一行 = 一次执行**」的台账
 * （9 列，含任务创建/开始/结束时间与可下载的处理结果，实测 235 条）；本系统原实现把它做成了
 * 「定时任务配置列表」（含「执行频率」），且因给 {@code BillTableList} 传了不存在的 prop
 * {@code apiUrl/params} 而**表格恒空**（开发文档 §1.1 / §12-P0）。</p>
 *
 * <p><b>数据源</b>：复用既有的 {@code scheduled_task_log}（库内**已有真实执行记录**，
 * 由 {@code cn.aiedge.scheduler.task.TaskExecutor} 在每次定时任务执行时写入），按开发文档 §7.3
 * 「路线 B」补齐台账列（见迁移 {@code V11.401.0}）。**不新建并行任务表** —— 依据见该迁移头部注释。</p>
 *
 * <p><b>端点（2 个）</b>：</p>
 * <ul>
 *   <li>{@code GET /page} 分页台账（查询条件仅「任务ID」，与对标查询区一致）</li>
 *   <li>{@code GET /{id}/result} 下载处理结果（**独立权限码**，产物可能含敏感数据）</li>
 * </ul>
 *
 * <p><b>租户可见性（本表在 {@code IGNORE_TENANT_TABLES} 中，拦截器不注入条件 → 必须自己判）</b>：
 * {@code tenant_id = 0 / NULL} 是**平台级**记录（定时任务由平台调度线程触发，无租户会话，
 * 与 {@code MyBatisPlusConfig} 对 scheduled_task* 的「平台级调度配置」口径一致），全租户可见；
 * 非 0 的行只对**发起方租户**可见。平台超管（{@code isTenantScopeExempt}）保持全局视野。</p>
 *
 * <p><b>不返回产物内容</b>：列表只回 {@code resultUrl}（鉴权地址），绝不把 {@code execute_result}
 * 大字段塞进列表（开发文档 §10.1-23 性能红线）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "系统任务（异步任务执行台账）", description = "账套操作 → 系统任务：分页台账 + 处理结果下载")
@RestController
@RequestMapping("/api/set/system-task")
@SaCheckLogin
@RequiredArgsConstructor
public class SystemTaskController {

    /** 权限码：台账查看（种子见迁移 V11.401.0） */
    private static final String PERM_VIEW = "set:system-task:view";
    /** 权限码：处理结果下载（独立鉴权：产物可能含敏感数据，不能只校验任务存在） */
    private static final String PERM_DOWNLOAD = "set:system-task:download";

    /**
     * 下载地址前缀（**相对路径**：前端 axios baseURL 已是 {@code /api}，
     * 这里若写 {@code /api/...} 会双前缀 404）。
     */
    private static final String RESULT_PATH_PREFIX = "/set/system-task/";

    /**
     * 排序白名单：前端字段名 → 数据库列名。
     * 只允许白名单内的列进 ORDER BY（杜绝把用户输入拼进 SQL）。
     */
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "taskId", "id",
            "createTime", "create_time",
            "startTime", "start_time",
            "endTime", "end_time");

    /** 状态别名 → 统一字符码（库内原值为 RUNNING/SUCCESS/FAILURE） */
    private static final Map<String, String> STATUS_ALIASES = buildStatusAliases();

    private final ScheduledTaskLogMapper logMapper;

    /**
     * 本地文件存储根目录（通道 1：`storage.local.base-path`，见 application-dev.yml）。
     *
     * <p>⚠️ 这里用 {@code @Value} 直接读配置，**不注入 {@code cn.aiedge.storage.config.StorageProperties}** ——
     * 该包（cn.aiedge.storage）从未进入 {@code AiReadyApplication.scanBasePackages}
     * （见《通用文件上传接口缺失》《上传根目录随启动目录漂移》），其 {@code @Component} 不是容器里的
     * Bean，注入会直接让应用启动失败。仅解析 {@code result_url} 相对路径不需要整套存储模块。</p>
     */
    @Value("${storage.local.base-path:./uploads}")
    private String storageBasePath;

    // ══════════════════════════════════════════════════════════════════════
    //  ① 分页台账
    // ══════════════════════════════════════════════════════════════════════

    @Operation(summary = "分页查询系统任务台账（查询条件仅任务ID，与对标一致）")
    @GetMapping("/page")
    @SaCheckPermission(PERM_VIEW)
    public Result<IPage<SystemTaskVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {

        Page<ScheduledTaskLog> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ScheduledTaskLog> wrapper = new QueryWrapper<>();

        // 租户可见性（本表被租户拦截器忽略 → 必须手工拼条件）
        applyTenantScope(wrapper);

        // 查询条件：仅「任务ID」精确匹配（对标查询区只有这一个输入框）
        String keyword = taskId == null ? "" : taskId.trim();
        if (StringUtils.hasText(keyword)) {
            if (!keyword.matches("\\d{1,19}")) {
                // 任务ID 是数字，输入非数字不可能命中 → 返回空页（而不是 500 或静默忽略条件）
                return Result.success(new Page<>(pageNum, pageSize));
            }
            wrapper.eq("id", Long.parseLong(keyword));
        }

        applySort(wrapper, sortField, sortOrder);
        logMapper.selectPage(page, wrapper);
        return Result.success(page.convert(this::toVO));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ② 下载处理结果
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 下载任务的处理结果。
     *
     * <p>产物取值顺序：① {@code result_url}（本地存储相对路径，相对
     * {@code storage.local.base-path}）→ 流式返回真实文件；② {@code execute_result}
     * （执行结果文本，由 {@code JobHandler} 自报）→ 以 UTF-8 文本文件返回。</p>
     *
     * <p><b>绝不返回假文件</b>：没有产物时按语义返回明确状态码 +
     * 可读原因（JSON），前端据此提示用户：</p>
     * <ul>
     *   <li>404 任务不存在 / 非本租户可见 / 文件不存在 / 该任务未产出处理结果</li>
     *   <li>409 任务尚未成功完成（含当前状态文案）</li>
     *   <li>410 处理结果已过期（越过 {@code result_expire_time}）</li>
     * </ul>
     */
    @Operation(summary = "下载任务处理结果（鉴权地址，非公开直链）")
    @GetMapping("/{id}/result")
    @SaCheckPermission(PERM_DOWNLOAD)
    public ResponseEntity<?> downloadResult(@PathVariable Long id) {
        ScheduledTaskLog row = logMapper.selectById(id);
        // 跨租户下载：一律按「不存在」处理，不泄露他人任务的存在性（开发文档 §10.1-27 越权红线）
        if (row == null || !visibleToCurrentTenant(row)) {
            return jsonError(HttpStatus.NOT_FOUND, "任务不存在或无权访问");
        }
        if (row.getResultExpireTime() != null && row.getResultExpireTime().isBefore(LocalDateTime.now())) {
            return jsonError(HttpStatus.GONE,
                    "处理结果已过期（保留至 " + row.getResultExpireTime() + "），请重新执行任务");
        }

        String status = normalizeStatus(row.getExecuteStatus());
        if (!"success".equals(status) && !"partial".equals(status)) {
            return jsonError(HttpStatus.CONFLICT,
                    "任务当前状态为「" + statusText(status) + "」，暂无可下载的处理结果");
        }

        // 产物 ①：result_url 指向的本地存储文件（相对 storage.local.base-path）
        if (StringUtils.hasText(row.getResultUrl())) {
            String url = row.getResultUrl().trim();
            if (isExternalUrl(url)) {
                // 外部存储地址不做服务端代理（避免开放重定向 / SSRF），如实告知用户
                return jsonError(HttpStatus.NOT_FOUND,
                        "处理结果存放于外部存储，请通过存储服务提供的鉴权地址访问");
            }
            Path file = resolveStoredFile(url);
            if (file == null) {
                return jsonError(HttpStatus.NOT_FOUND, "处理结果文件不存在或已被清理：" + url);
            }
            return fileResponse(file);
        }

        // 产物 ②：执行结果文本（TaskExecutor 已落库的真实内容）
        if (!StringUtils.hasText(row.getExecuteResult())) {
            return jsonError(HttpStatus.NOT_FOUND, "该任务未产出处理结果");
        }
        byte[] bytes = row.getExecuteResult().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("任务结果-" + id + ".txt", StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  内部方法
    // ══════════════════════════════════════════════════════════════════════

    /** 行 → 台账 VO（9 列契约 + 失败原因） */
    private SystemTaskVO toVO(ScheduledTaskLog row) {
        SystemTaskVO vo = new SystemTaskVO();
        vo.setTaskId(row.getId());
        vo.setTaskType(row.getTaskType());
        vo.setTaskName(row.getTaskName());
        String status = normalizeStatus(row.getExecuteStatus());
        vo.setStatus(status);
        vo.setCreatedByName(row.getCreatedByName());
        vo.setCreateTime(row.getCreateTime());
        vo.setStartTime(row.getStartTime());
        vo.setEndTime(row.getEndTime());
        // 「结果查看」列：只有确有产物且未过期的成功行才给鉴权下载地址
        vo.setResultUrl(downloadable(row, status)
                ? RESULT_PATH_PREFIX + row.getId() + "/result" : null);
        vo.setErrorMsg(row.getErrorMessage());
        return vo;
    }

    /** 是否可下载：执行成功（或部分成功）且存在产物、且未过期 */
    private boolean downloadable(ScheduledTaskLog row, String status) {
        if (!"success".equals(status) && !"partial".equals(status)) {
            return false;
        }
        if (row.getResultExpireTime() != null && row.getResultExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        return StringUtils.hasText(row.getResultUrl()) || StringUtils.hasText(row.getExecuteResult());
    }

    /**
     * 租户可见性判定（本表在 {@code IGNORE_TENANT_TABLES} 中，拦截器不注入条件）。
     *
     * <p>0 / NULL = 平台级（定时任务自动执行），全租户可见；非 0 = 发起方租户。</p>
     */
    private boolean visibleToCurrentTenant(ScheduledTaskLog row) {
        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            return true;  // 平台超管：全局视野
        }
        Long owner = row.getTenantId();
        if (owner == null || owner == 0L) {
            return true;
        }
        Long current = MyBatisPlusConfig.getCurrentTenantIdValue();
        return current != null && current.equals(owner);
    }

    /** 分页查询的租户条件（与 {@link #visibleToCurrentTenant} 同一口径） */
    private void applyTenantScope(QueryWrapper<ScheduledTaskLog> wrapper) {
        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            return;
        }
        Long current = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (current == null) {
            // 兜底：无会话时只看平台级记录（端点本身有 @SaCheckLogin）
            wrapper.and(w -> w.eq("tenant_id", 0L).or().isNull("tenant_id"));
            return;
        }
        wrapper.and(w -> w.eq("tenant_id", 0L).or().isNull("tenant_id").or().eq("tenant_id", current));
    }

    /** 排序（白名单；默认按任务创建时间倒序，与对标首行为最新任务的形态一致） */
    private void applySort(QueryWrapper<ScheduledTaskLog> wrapper, String sortField, String sortOrder) {
        String column = sortField == null ? null : SORT_COLUMNS.get(sortField);
        boolean asc = "ascend".equalsIgnoreCase(sortOrder) || "asc".equalsIgnoreCase(sortOrder);
        if (column == null) {
            wrapper.orderByDesc("create_time").orderByDesc("id");
            return;
        }
        wrapper.orderBy(true, asc, column);
        // 次级排序保证同值行分页稳定
        if (!"id".equals(column)) {
            wrapper.orderByDesc("id");
        }
    }

    /**
     * 把 {@code result_url} 解析为 storage 根目录下的真实文件；不存在 / 越界（目录穿越）时返回 null。
     */
    private Path resolveStoredFile(String relativePath) {
        try {
            Path base = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path file = base.resolve(relativePath).normalize();
            if (!file.startsWith(base)) {
                log.warn("[系统任务] 拒绝越界的结果文件路径: {}", relativePath);
                return null;
            }
            return Files.isRegularFile(file) && Files.isReadable(file) ? file : null;
        } catch (RuntimeException e) {
            log.warn("[系统任务] 结果文件路径非法: {}", relativePath, e);
            return null;
        }
    }

    /** 本地文件响应（文件名按 UTF-8 编码，兼容中文） */
    private ResponseEntity<Resource> fileResponse(Path file) {
        String filename = file.getFileName().toString();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(file);
            if (StringUtils.hasText(probed)) {
                mediaType = MediaType.parseMediaType(probed);
            }
        } catch (IOException ignored) {
            // 探测失败不影响下载，退回二进制流
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(mediaType)
                .body(new FileSystemResource(file));
    }

    /** 统一的 JSON 错误响应（下载端点不能返回 Result 成功结构，但错误必须是可读 JSON） */
    private ResponseEntity<Result<Void>> jsonError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.error(status.value(), message));
    }

    private boolean isExternalUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /**
     * 任务状态归一：库内 {@code RUNNING/SUCCESS/FAILURE} → 统一字符码闭集。
     * 未识别的原值一律归 {@code unknown}（保证状态值域封闭，前端不会漏映射）。
     */
    private static String normalizeStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "unknown";
        }
        return STATUS_ALIASES.getOrDefault(raw.trim().toLowerCase(Locale.ROOT), "unknown");
    }

    /** 状态中文文案（用于下载端点的可读提示） */
    private static String statusText(String status) {
        switch (status) {
            case "pending": return "待执行";
            case "running": return "执行中";
            case "success": return "成功";
            case "failed": return "失败";
            case "partial": return "部分成功";
            default: return "未知";
        }
    }

    private static Map<String, String> buildStatusAliases() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("pending", "pending");
        map.put("running", "running");
        map.put("success", "success");
        map.put("succeeded", "success");
        map.put("ok", "success");
        map.put("failure", "failed");
        map.put("fail", "failed");
        map.put("failed", "failed");
        map.put("error", "failed");
        map.put("partial", "partial");
        return map;
    }
}
