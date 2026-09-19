package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.PromoteProductRow;
import cn.aiedge.erp.marketing.mapper.MarketingQueryMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我要推广（营销 → 营销推广 → 我要推广，菜单 80330）
 *
 * <p>本控制器只提供「商品」Tab 的物料列表（商品主数据 × 库存 × 最近销售 × 分享统计）；
 * 其余物料 Tab 复用既有端点 + {@code /erp/marketing/share/summary} 在页面侧合并：
 * 优惠券 → coupon-template、促销 → promotion-activity、拼团 → group-buy/activity、秒杀 → flash-sale。</p>
 */
@Slf4j
@Tag(name = "我要推广物料")
@RestController
@RequestMapping("/api/erp/marketing/promote")
@RequiredArgsConstructor
public class PromoteController {

    private final MarketingQueryMapper marketingQueryMapper;

    @Operation(summary = "分页查询可推广商品（含库存、最近销售时间与分享统计）")
    @GetMapping("/product/page")
    public Result<IPage<PromoteProductRow>> productPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        return Result.ok(marketingQueryMapper.selectPromoteProductPage(
                new Page<>(pageNum, pageSize), tenantId, keyword));
    }
}
