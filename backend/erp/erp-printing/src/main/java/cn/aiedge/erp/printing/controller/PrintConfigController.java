package cn.aiedge.erp.printing.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.erp.printing.dto.PrintConfigRequest;
import cn.aiedge.erp.printing.entity.SetPrintConfig;
import cn.aiedge.erp.printing.service.PrintConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 打印设置控制器（设置 → 打印管理 → 打印设置，菜单 80930）
 *
 * <p>前缀 {@code /api/set/print-config} —— 沿用设置模块的路径口径（如 {@code /api/set/initial-stock}、
 * 以及本模块其它页的 {@code /api/payment/config}、{@code /api/config} 同风格），
 * 但与它们**不共用表**：本页走专用表 {@code set_print_config}（开发文档 §7.3 路线 B）。</p>
 *
 * <p><b>为什么放在 {@code cn.aiedge.erp.printing} 包</b>：该包已在
 * {@code AiReadyApplication.scanBasePackages} 中装配（无需改启动类），且本页属打印域；
 * 打印引擎（模板/链路/客户端/任务）的既有端点 {@code /api/v1/print/*}、{@code /api/v2/print/*}
 * **一律直接复用**，本控制器不重复实现任何打印执行能力。</p>
 *
 * <p><b>租户</b>：一律由服务端从 Sa-Token 会话解析（{@code MyBatisPlusConfig.getCurrentTenantIdValue()}），
 * 请求体与查询串里**都没有 tenantId** —— 客户端无从跨租户写入（开发文档 §6.1-1 / §10.1-12）。</p>
 *
 * <p><b>配置如何被消费</b>（2026-09-18 接线，取代此前「7 个配置项尚未被消费」的登记）：
 * 前端跨模块共享组件 {@code components/PrintDialog/printBehavior.ts} 读取本接口后，在**渲染前**作用于打印数据：
 * 允许打印草稿 → 草稿单据的打印门控；单据打印小数位数 → 数量/单价列的位数格式化；
 * 打印内容 → 明细行「批次效期」文本的拼接；助手打印 → 跳过「必须先预览」；远程打印 → 远程打印模式的门控与链路提交。
 * 未接线项与原因见 {@code printBehavior.ts} 顶部说明（属性/批次效期汇总打印：打印引擎无汇总行能力）。</p>
 *
 * <p><b>三个端点的分工</b>：{@code GET /} 供「打印设置」页（需 {@code set:print-config:view}，含 options 取值域）；
 * {@code GET /behavior} 供**打印组件**（无需管理权限，只回行为字段 —— 配置要作用于所有人的打印）；
 * {@code PUT /} 保存（需 {@code set:print-config:update}）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/set/print-config")
@RequiredArgsConstructor
@Tag(name = "打印设置", description = "租户级打印配置（设置 → 打印管理 → 打印设置）")
public class PrintConfigController {

    private final PrintConfigService printConfigService;

    /**
     * 「打印内容」的候选值：**3 项，逐字取自 ql361 实测**（2026-09-18 展开下拉浮层实测，
     * 证据 {@code tool-results/ql361/设置-deep/_summary.md} §三 3.1「『打印内容』下拉完整选项集」：
     * 选项数 3、当前值「批号 *数量」rawValue=1）。
     *
     * <p>此前只有 1 项（当时选项集未实测，按「不造选项」纪律只登记选中值）——本段注释与常量同步更新，
     * 缺口（开发文档 §5.1）就此关闭。</p>
     *
     * <p>选项含义（ql361 帮助文案原文：「启用批次效期汇总打印后，会增加"批次效期"打印字段，
     * 打印的内容受此设置影响」）：决定单据明细行「批次效期」文本由 批号 / 生产日期 / 到期日期 与数量
     * 如何拼接。前端 {@code PrintDialog} 据此在渲染数据中派生 {@code batchEffectiveText} 字段，
     * 拼接口径与字段映射逐字对应本常量的三项文案（见 {@code components/PrintDialog/printBehavior.ts}）。</p>
     */
    private static final List<Map<String, String>> PRINT_CONTENT_OPTIONS = List.of(
            Map.of("value", "批号 *数量", "label", "批号 *数量"),
            Map.of("value", "生产日期 *数量", "label", "生产日期 *数量"),
            Map.of("value", "批号 生产日期~到期日期 *数量", "label", "批号 生产日期~到期日期 *数量"));

    /**
     * 打印助手的下载地址（配置落位：**通道 1/2** —— 环境变量或 core-api 的 application.yml，
     * 如 {@code set.print.assistant-download-url}）。
     *
     * <p>刻意**不入库**：它是部署环境相关的外部地址（换域名/换版本不该改数据），
     * 且对标把它硬编码在前端，本系统改为服务端下发（开发文档 §4.6-4 / §11）。</p>
     *
     * <p>未配置时保持空串 → {@code enabled=false} → 前端降级为「联系管理员获取安装包」，
     * **不得展示空链接**（开发文档 §11「缺失降级」）。</p>
     */
    @Value("${set.print.assistant-download-url:}")
    private String assistantDownloadUrl;

    /** 打印助手版本号（可选，仅用于前端展示；未配置时返回空串） */
    @Value("${set.print.assistant-version:}")
    private String assistantVersion;

    /** 读取打印设置（无行则按默认值自动建行） */
    @GetMapping
    @SaCheckPermission("set:print-config:view")
    @Operation(summary = "读取打印设置")
    public ResponseEntity<Map<String, Object>> getPrintConfig() {
        Long tenantId = resolveTenantId();
        SetPrintConfig config = printConfigService.getOrCreate(tenantId, resolveUserId());
        return ResponseEntity.ok(toView(config));
    }

    /**
     * 读取「打印行为」配置（**打印组件专用**，刻意不要求 {@code set:print-config:view}）。
     *
     * <p><b>为什么需要这个只读端点</b>：本页的配置要作用于**所有人的打印**，而
     * {@code set:print-config:view} 只授给了超级管理员（迁移 V11.402.0 §③ 的口径）——
     * 若打印组件直接调管理端点，普通账号（仓库/配送/资料页的用户）会拿到 403，
     * 并被前端 axios 拦截器弹成「没有操作权限」的全局提示，而配置对这些人**永远不生效**。
     * 因此这里单开一个「只读、只回行为字段」的端点：任何**已登录**用户都可读，读到的仍是
     * **本会话租户**的配置（tenantId 由服务端从会话解析，请求里没有该入参，杜绝跨租户读取）。</p>
     *
     * <p><b>只回行为字段</b>：7 个业务项 + 无 id/租户/操作人/时间等管理信息，最小暴露面。
     * 与 {@link #getPrintConfig()} 的分工：管理端点给「打印设置」页（含 options 取值域、审计信息），
     * 本端点给打印弹窗（只要行为字段）。</p>
     *
     * <p>无行时与 {@link #getPrintConfig()} 同口径按默认值建行（一行一租户，唯一索引兜底）。</p>
     */
    @SaCheckPermission("set:print-config:view")
    @GetMapping("/behavior")
    @Operation(summary = "读取打印行为配置（打印组件专用，无需管理权限）")
    public ResponseEntity<Map<String, Object>> getBehaviorConfig() {
        Long tenantId = resolveTenantId();
        SetPrintConfig config = printConfigService.getOrCreate(tenantId, resolveUserId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allowDraftPrint", toBool(config.getAllowDraftPrint()));
        body.put("attrSummaryPrint", toBool(config.getAttrSummaryPrint()));
        body.put("batchSummaryPrint", toBool(config.getBatchSummaryPrint()));
        body.put("printContent", config.getPrintContent());
        body.put("decimalEnabled", toBool(config.getDecimalEnabled()));
        body.put("qtyDecimal", config.getQtyDecimal());
        body.put("priceDecimal", config.getPriceDecimal());
        body.put("assistantEnabled", toBool(config.getAssistantEnabled()));
        body.put("remoteEnabled", toBool(config.getRemoteEnabled()));
        return ResponseEntity.ok(body);
    }

    /** 保存打印设置（部分更新；保存后回读返回库内值） */
    @PutMapping
    @SaCheckPermission("set:print-config:update")
    @OperationLog(module = "打印设置", type = "UPDATE", desc = "保存打印设置")
    @Operation(summary = "保存打印设置")
    public ResponseEntity<Map<String, Object>> savePrintConfig(@RequestBody PrintConfigRequest request) {
        Long tenantId = resolveTenantId();
        SetPrintConfig saved = printConfigService.save(tenantId, request, resolveUserId());
        return ResponseEntity.ok(toView(saved));
    }

    /** 打印助手信息（下载地址由服务端下发；未配置时 enabled=false，前端须降级提示） */
    @GetMapping("/assistant")
    @SaCheckPermission("set:print-config:view")
    @Operation(summary = "获取打印助手下载信息")
    public ResponseEntity<Map<String, Object>> getAssistantInfo() {
        boolean configured = StringUtils.hasText(assistantDownloadUrl);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("downloadUrl", configured ? assistantDownloadUrl.trim() : "");
        body.put("version", assistantVersion == null ? "" : assistantVersion.trim());
        body.put("enabled", configured);
        return ResponseEntity.ok(body);
    }

    /**
     * 实体 → 前端视图（字段名与前端 {@code api/set/print-config.ts} 的 TS 类型一一对应）。
     *
     * <p>布尔以 {@code Boolean} 输出（库内是 0/1 的 INTEGER），options 一并下发，
     * 让前端「不写死取值域」。</p>
     */
    private Map<String, Object> toView(SetPrintConfig config) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", config.getId());
        body.put("tenantId", config.getTenantId());
        body.put("allowDraftPrint", toBool(config.getAllowDraftPrint()));
        body.put("attrSummaryPrint", toBool(config.getAttrSummaryPrint()));
        body.put("batchSummaryPrint", toBool(config.getBatchSummaryPrint()));
        body.put("printContent", config.getPrintContent());
        body.put("decimalEnabled", toBool(config.getDecimalEnabled()));
        body.put("qtyDecimal", config.getQtyDecimal());
        body.put("priceDecimal", config.getPriceDecimal());
        body.put("assistantEnabled", toBool(config.getAssistantEnabled()));
        body.put("remoteEnabled", toBool(config.getRemoteEnabled()));
        body.put("updateTime", config.getUpdateTime());

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("printContent", PRINT_CONTENT_OPTIONS);
        options.put("decimalMin", PrintConfigService.DECIMAL_MIN);
        options.put("decimalMax", PrintConfigService.DECIMAL_MAX);
        body.put("options", options);
        return body;
    }

    /** 0/1 → 布尔（null 视为关闭，避免前端出现 undefined 空控件） */
    private Boolean toBool(Integer value) {
        return value != null && value == 1;
    }

    /**
     * 当前租户：只认登录会话，**不认请求头 X-Tenant-Id**（防跨租户覆盖）。
     * 会话不可用时兜底 1（与 {@code PrintChainController.resolveTenantId} 同口径）。
     */
    private Long resolveTenantId() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return tenantId != null ? tenantId : 1L;
    }

    /** 操作人（写 create_by / update_by；未登录上下文返回 null，不阻断保存） */
    private Long resolveUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.warn("无法从会话获取 userId: {}", e.getMessage());
            return null;
        }
    }
}
