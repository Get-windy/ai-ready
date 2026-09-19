package cn.aiedge.payment.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.payment.dto.PaymentChannelConfigVO;
import cn.aiedge.payment.dto.PaymentChannelParam;
import cn.aiedge.payment.dto.PaymentConfigItemVO;
import cn.aiedge.payment.dto.PaymentSceneVO;
import cn.aiedge.payment.service.PaymentConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 支付配置控制器（设置 → 系统配置 → 支付配置，菜单 80623 / {@code set:payment-config}）。
 *
 * <p>本页 4 个 Tab 的配置值全部**真实读写** {@code sys_project_config}（见
 * {@code PaymentConfigServiceImpl} 的类注释），替代原先「借用 {@code /api/config}
 * 写缓存、读不回」的假保存链路（开发文档 §3.7 / §12-①）。</p>
 *
 * <p>端点分工：</p>
 * <ul>
 *   <li>{@code /channels*} —— Tab② 支付方式（渠道参数 CRUD）；</li>
 *   <li>{@code /items*} —— Tab① 微信公众号配置 / Tab④ 在线退款（配置项 KV）；</li>
 *   <li>{@code /scenes*} —— Tab③ 场景配置（场景 × 渠道矩阵）；</li>
 *   <li>{@code /refund-notes} —— Tab④ 的两段说明文案（逐字对标 ql361）。</li>
 * </ul>
 */
@Tag(name = "支付配置", description = "支付配置（设置 → 系统配置 → 支付配置，4 个 Tab）")
@RestController
@RequestMapping("/api/payment/config")
@RequiredArgsConstructor
@Validated
public class PaymentConfigController {

    private final PaymentConfigService paymentConfigService;

    // ═══════════════════════════════════════════════════════════════════
    // Tab② 支付方式：渠道参数
    // ═══════════════════════════════════════════════════════════════════

    @Operation(summary = "支付渠道配置列表")
    @GetMapping("/channels")
    @SaCheckPermission("payment:config:list")
    public Result<List<PaymentChannelConfigVO>> listChannels(
            @Parameter(description = "渠道编码/名称（模糊）") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        return Result.success(paymentConfigService.listChannelConfigs(keyword, enabled));
    }

    @Operation(summary = "查询单渠道参数（编辑回填，含密钥原文）")
    @GetMapping("/channels/{channelCode}")
    @SaCheckPermission("payment:config:list")
    public Result<PaymentChannelParam> getChannelParam(@PathVariable String channelCode) {
        return Result.success(paymentConfigService.getChannelParam(channelCode));
    }

    @Operation(summary = "保存单渠道参数（真实落库）")
    @PostMapping("/channels/{channelCode}")
    @SaCheckPermission("payment:config:update")
    public Result<Void> saveChannelParam(@PathVariable String channelCode,
                                         @RequestBody(required = false) PaymentChannelParam param) {
        paymentConfigService.saveChannelParam(channelCode, param);
        return Result.success();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tab① 微信公众号配置 / Tab④ 在线退款：配置项
    // ═══════════════════════════════════════════════════════════════════

    @Operation(summary = "配置项列表（tab=wechat 微信公众号配置 / tab=refund 在线退款）")
    @GetMapping("/items")
    @SaCheckPermission("payment:config:list")
    public Result<List<PaymentConfigItemVO>> listItems(
            @Parameter(description = "配置分组：wechat / refund") @RequestParam String tab,
            @Parameter(description = "配置项名称/键（模糊）") @RequestParam(required = false) String keyword) {
        return Result.success(paymentConfigService.listItems(tab, keyword));
    }

    @Operation(summary = "保存单个配置项（键必须在白名单内）")
    @PostMapping("/items")
    @SaCheckPermission("payment:config:update")
    public Result<Void> saveItem(@RequestBody Map<String, String> body) {
        paymentConfigService.saveItem(body.get("itemKey"), body.get("itemValue"));
        return Result.success();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tab③ 场景配置
    // ═══════════════════════════════════════════════════════════════════

    @Operation(summary = "场景配置列表")
    @GetMapping("/scenes")
    @SaCheckPermission("payment:config:list")
    public Result<List<PaymentSceneVO>> listScenes(
            @Parameter(description = "场景编码/名称（模糊）") @RequestParam(required = false) String keyword) {
        return Result.success(paymentConfigService.listScenes(keyword));
    }

    @Operation(summary = "保存场景启用的支付渠道")
    @PostMapping("/scenes/{sceneCode}")
    @SaCheckPermission("payment:config:update")
    public Result<Void> saveScene(@PathVariable String sceneCode,
                                  @RequestBody(required = false) Map<String, List<String>> body) {
        List<String> channels = body != null ? body.get("channels") : null;
        paymentConfigService.saveScene(sceneCode, channels);
        return Result.success();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tab④ 在线退款：说明文案（后端下发，避免前端硬编码后与对标文案漂移）
    // ═══════════════════════════════════════════════════════════════════

    @Operation(summary = "在线退款说明文案")
    @GetMapping("/refund-notes")
    @SaCheckPermission("payment:config:list")
    public Result<List<String>> refundNotes() {
        return Result.success(paymentConfigService.refundNotes());
    }
}
