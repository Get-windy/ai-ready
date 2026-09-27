package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dao.MallTagOption;
import cn.aiedge.erp.b2b.dao.MallTagOptionMapper;
import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.support.MallGuestAccess;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商城 C 端「商品标签」接口。
 *
 * <p>用途：<b>分类页最顶部的商品标签 Tab</b>（用户 2026-09-26 拍板口径），
 * 以及给商品卡角标提供 {@code tagCode → tagName} 的翻译表。</p>
 *
 * <p><b>为什么不做权限注解</b>：标签是"分类维度"，游客进店就要能看到
 * （否则分类页顶部一片空）。准入复用 {@code MallGuestAccess.requireShop()}
 * ——未开通游客的店铺仍会被 403 拒绝。需在 {@code SaTokenConfig} 匿名白名单登记
 * {@code /api/v1/mall/tags}。</p>
 *
 * <p>数据源是 {@code erp_mall_tag}（现有 20 条，{@code tag_code} 形如 {@code TAG_n}），
 * 由管理端「商品上架 → 商品标签」维护。</p>
 */
@RestController
@RequestMapping("/api/v1/mall/tags")
@Tag(name = "商城-商品标签", description = "标签下发（游客可达，用于分类页顶部标签栏与商品卡角标）")
@RequiredArgsConstructor
public class MallTagController {

    private final MallTagOptionMapper mallTagOptionMapper;
    private final MallGuestAccess guestAccess;

    @Operation(summary = "商品标签列表", description = "返回启用中的标签（按 sort_order 升序），供分类页顶部标签栏与商品卡角标使用")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        // 与商品/店铺配置同一套准入：未开通游客的店铺直接 403，不泄露标签
        guestAccess.requireShop();

        List<MallTagOption> tags = mallTagOptionMapper.selectList(
                new LambdaQueryWrapper<MallTagOption>()
                        .eq(MallTagOption::getStatus, 1)
                        .orderByAsc(MallTagOption::getSortOrder)
                        .orderByAsc(MallTagOption::getId));

        // 只回必要字段（不下发 tenant_id / 审计列），字段名与前端 TS 类型对齐
        List<Map<String, Object>> out = tags.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tagCode", t.getTagCode());
            m.put("tagName", t.getTagName());
            m.put("sortOrder", t.getSortOrder());
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.success(out);
    }
}
