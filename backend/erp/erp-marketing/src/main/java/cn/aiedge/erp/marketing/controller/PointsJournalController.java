package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.PointsJournalRow;
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
 * 会员积分明细（营销 → 会员中心 → 会员管理 →「积分明细」）
 * 数据源：erp_sale_order_points_journal（与销售订单「会员信息」Tab 同源读写）
 */
@Slf4j
@Tag(name = "会员积分明细")
@RestController
@RequestMapping("/api/erp/marketing/points")
@RequiredArgsConstructor
public class PointsJournalController {

    private final MarketingQueryMapper marketingQueryMapper;

    @Operation(summary = "分页查询会员积分明细")
    @GetMapping("/page")
    public Result<IPage<PointsJournalRow>> page(
            @RequestParam(required = false) String memberCardNo,
            @RequestParam(required = false) String memberName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        return Result.ok(marketingQueryMapper.selectPointsJournalPage(
                new Page<>(pageNum, pageSize), tenantId, memberCardNo, memberName));
    }
}
