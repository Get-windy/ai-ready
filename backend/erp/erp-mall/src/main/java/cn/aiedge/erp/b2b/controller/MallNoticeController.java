package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.model.MallNotice;
import cn.aiedge.erp.b2b.service.MallNoticeService;
import cn.aiedge.erp.b2b.support.MallGuestAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商城公告（C 端，仅返回已发布公告）。
 *
 * <p><b>2026-09-26 变更（审计 F-04）</b>：路径由 {@code /api/erp/mall/notice/list}
 * 迁到 {@code /api/v1/mall/notice/list}。原路径有两处问题：</p>
 * <ol>
 *   <li>挂在 {@code /api/erp/mall/**}（管理端前缀）下，但语义是「商城端公开查询」；</li>
 *   <li>注释自称"公开查询"，实际**不在匿名白名单里 ⇒ 匿名调用 401**
 *       （详见 TRADE_MODULE_AUDIT_20260923.md P2-4），而 C 端首页公告恰恰要在未登录时展示。</li>
 * </ol>
 * <p>现已在 {@code SaTokenConfig} 白名单登记 {@code /api/v1/mall/notice/**}，
 * 准入复用 {@code MallGuestAccess.requireShop()}（未开通游客的店铺仍 403）。
 * 原路径已无任何调用方（前端全仓 0 引用），故直接迁移、不保留别名。</p>
 */
@RestController
@RequestMapping("/api/v1/mall/notice")
@Tag(name = "商城-公告", description = "商城端已发布公告查询（游客可达）")
@RequiredArgsConstructor
public class MallNoticeController {

    private final MallNoticeService mallNoticeService;
    private final MallGuestAccess guestAccess;

    @GetMapping("/list")
    @Operation(summary = "查询已发布公告列表", description = "首页公告滚动条数据源；仅返回已发布（status=1）公告")
    public ApiResponse<List<Map<String, Object>>> list(
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "10") Integer limit) {
        // 与商品/标签/店铺配置同一套准入
        guestAccess.requireShop();

        // 只回 C 端需要的字段（不下发 create_by/update_by 等审计列）
        List<Map<String, Object>> out = mallNoticeService.listPublished(limit).stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("noticeType", n.getNoticeType());
            m.put("publishTime", n.getPublishTime());
            m.put("publisher", n.getPublisher());
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.success(out);
    }
}
