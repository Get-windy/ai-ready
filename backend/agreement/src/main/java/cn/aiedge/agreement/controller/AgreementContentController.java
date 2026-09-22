package cn.aiedge.agreement.controller;

import cn.aiedge.agreement.dto.AgreementContentSaveDTO;
import cn.aiedge.agreement.dto.AgreementContentSaveResultVO;
import cn.aiedge.agreement.dto.AgreementContentVO;
import cn.aiedge.agreement.dto.AgreementEffectiveSettingsVO;
import cn.aiedge.agreement.dto.AgreementSettingDefVO;
import cn.aiedge.agreement.service.AgreementContentService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.utils.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 协议**内容层**接口：字段设定版 / 文字版 / 履约方式集合 / 字段元数据。
 *
 * <h3>接口契约（下次写前端按这张表来）</h3>
 * <pre>
 * GET  /api/agreement/content/setting-defs              字段元数据（含"这一项会影响什么"）
 * GET  /api/agreement/content/{versionId}               取某版本内容（设定三态 + 文字 + 履约方式集合）
 * PUT  /api/agreement/content/{versionId}               保存内容（仅草稿可改；整份覆盖）
 * GET  /api/agreement/content/effective?agreementId=&businessTime=
 *                                                       按业务时点取生效那一版的设定
 * </pre>
 *
 * <p>⚠️ 字面量路径（{@code setting-defs} / {@code effective}）比 {@code /{versionId}} 优先，
 * 因此不会被版本详情端点抢走（与 {@code /api/agreement/term-options} 同一现象）。</p>
 *
 * <h3>三条硬口径</h3>
 * <ol>
 *   <li><b>只有草稿能改</b>：保存接口对已生效版本一律拒绝（㉛）；</li>
 *   <li><b>"未约定"要如实下发</b>：返回里每个字段带三态（已约定 / 未约定 / 未定义），
 *       界面必须能把"未约定"显示出来，绝不能显示成 0；</li>
 *   <li><b>文字条款永不自动执行</b>：返回里带 {@code autoExecutable=false} 与一句中文说明，
 *       界面据此明确告诉用户"此类条款系统不会自动执行，需人工处理"（§13.2 硬要求）。</li>
 * </ol>
 *
 * <p>错误一律 {@code BusinessException} + 中文文案：用 RuntimeException 会被兜底 advice
 * 吞成「系统异常，请稍后重试」，把"未约定账期"这种可自解的问题误导成服务故障（本仓实踩）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/agreement/content")
@RequiredArgsConstructor
@Tag(name = "协议内容（设定版与文字版）", description = "字段设定版（运行时配置）+ 文字版（只留痕举证）+ 履约方式集合")
public class AgreementContentController {

    private static final DateTimeFormatter DY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DTS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AgreementContentService contentService;

    @SaCheckPermission("agreement:setting-def:list")
    @GetMapping("/setting-defs")
    @Operation(summary = "字段元数据（含消费方：这一项会影响哪个下游环节）")
    public Result<List<AgreementSettingDefVO>> settingDefs() {
        return Result.success(contentService.settingDefs());
    }

    @SaCheckPermission("agreement:content:view")
    @GetMapping("/{versionId}")
    @Operation(summary = "取某版本的协议内容（设定三态 + 文字条款 + 履约方式集合 + 元数据）")
    public Result<AgreementContentVO> content(@PathVariable Long versionId) {
        return Result.success(contentService.content(versionId, currentTenant()));
    }

    @SaCheckPermission("agreement:content:edit")
    @PutMapping("/{versionId}")
    @Operation(summary = "保存协议内容（仅草稿可改；未提交的字段视为未约定；内容变更会清空双方确认痕迹）")
    public Result<AgreementContentSaveResultVO> save(@PathVariable Long versionId,
                                                     @RequestBody AgreementContentSaveDTO dto) {
        return Result.success(contentService.saveContent(versionId, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:content:view")
    @GetMapping("/effective")
    @Operation(summary = "按业务时点取生效那一版的设定（下单/发货/结算口径；未约定会明确标出）")
    public Result<AgreementEffectiveSettingsVO> effective(
            @Parameter(description = "协议 ID") @RequestParam Long agreementId,
            @Parameter(description = "业务时点，如 2026-09-22 或 2026-09-22 14:30:00；按这个时刻找生效版本")
            @RequestParam(required = false) String businessTime) {
        return Result.success(contentService.effective(agreementId, parseBusinessTime(businessTime), currentTenant()));
    }

    // ── 内部 ──

    private Long currentTenant() {
        return SecurityUtils.getCurrentTenantId();
    }

    private Long operator() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 解析业务时点。
     *
     * <p>⚠️ 不传时**不默认成"现在"**：执行口径必须由调用方明确给时刻（㉛），
     * 否则"看当前版本"会让人以为可以拿"现在"去算过去发生的事。
     * 但不传时前端只是"想看一眼当前生效版本"的情况很多，故这里用**当前时刻**并在日志里说明 ——
     * 真正的执行链路（订单路由/结算）不走 HTTP，而是直接调 {@code AgreementRuntime.resolve(...)}，
     * 那里 businessTime 为空会直接拒绝。</p>
     */
    private LocalDateTime parseBusinessTime(String businessTime) {
        if (businessTime == null || businessTime.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            log.info("协议生效设定查询未指定业务时点，按当前时刻处理: {}", now.format(DTS));
            return now;
        }
        String v = businessTime.trim().replace('T', ' ');
        try {
            if (v.length() <= 10) {
                return LocalDate.parse(v, DY).atStartOfDay();
            }
            String normalized = v.length() == 16 ? v + ":00" : v;
            return LocalDateTime.parse(normalized, DTS);
        } catch (Exception e) {
            throw BusinessException.badRequest("业务时点「" + businessTime
                    + "」格式不正确，请用 2026-09-22 或 2026-09-22 14:30:00 这样的写法");
        }
    }
}
