package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.PointsExchangeRow;
import cn.aiedge.erp.marketing.entity.PointsExchangeProduct;
import cn.aiedge.erp.marketing.mapper.PointsExchangeProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 积分兑换（营销 → 会员中心 → 积分兑换，菜单 80301）
 * 维护「哪些商品可用积分兑换、各需多少积分」；兑换办理落在销售开单。
 */
@Slf4j
@Tag(name = "积分兑换目录")
@RestController
@RequestMapping("/api/erp/marketing/points-exchange")
@RequiredArgsConstructor
public class PointsExchangeController {

    private final PointsExchangeProductMapper mapper;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    @Operation(summary = "分页查询可兑换商品")
    @SaCheckPermission("marketing:points-exchange:list")
    @GetMapping("/page")
    public Result<IPage<PointsExchangeRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(mapper.selectExchangePage(new Page<>(pageNum, pageSize), tenantId(), keyword));
    }

    @Operation(summary = "查询某商品是否已在兑换目录")
    @SaCheckPermission("marketing:points-exchange:detail")
    @GetMapping("/product/{productId}")
    public Result<PointsExchangeProduct> getByProduct(@PathVariable Long productId) {
        return Result.ok(mapper.selectOne(new LambdaQueryWrapper<PointsExchangeProduct>()
                .eq(PointsExchangeProduct::getProductId, productId)
                .orderByAsc(PointsExchangeProduct::getId)
                .last("limit 1")));
    }

    @Operation(summary = "新增兑换商品")
    @SaCheckPermission("marketing:points-exchange:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody PointsExchangeProduct req) {
        if (req.getProductId() == null) throw new IllegalArgumentException("请先选择商品");
        if (req.getExchangePoints() == null || req.getExchangePoints().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("兑换所需积分须为非负数值");
        }
        Long exist = mapper.selectCount(new LambdaQueryWrapper<PointsExchangeProduct>()
                .eq(PointsExchangeProduct::getProductId, req.getProductId()));
        if (exist != null && exist > 0) {
            throw new IllegalArgumentException("该商品已在积分兑换目录中");
        }
        req.setId(null);
        req.setTenantId(tenantId());
        if (req.getStatus() == null) req.setStatus(1);
        if (req.getSort() == null) req.setSort(0);
        req.setCreateTime(LocalDateTime.now());
        req.setUpdateTime(LocalDateTime.now());
        return Result.ok(mapper.insert(req) > 0);
    }

    @Operation(summary = "修改兑换商品（兑换所需积分 / 排序 / 状态 / 备注）")
    @SaCheckPermission("marketing:points-exchange:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PointsExchangeProduct req) {
        req.setId(id);
        req.setTenantId(null);
        req.setProductId(null);
        req.setUpdateTime(LocalDateTime.now());
        return Result.ok(mapper.updateById(req) > 0);
    }

    @Operation(summary = "移出兑换目录")
    @SaCheckPermission("marketing:points-exchange:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(mapper.deleteById(id) > 0);
    }

    @Operation(summary = "批量新增兑换商品（从商品选择器选品入目录）")
    @SaCheckPermission("marketing:points-exchange:create")
    @PostMapping("/batch")
    public Result<Integer> batchCreate(@RequestBody List<PointsExchangeProduct> list) {
        if (list == null || list.isEmpty()) throw new IllegalArgumentException("请先选择商品");
        int n = 0;
        for (PointsExchangeProduct req : list) {
            if (req.getProductId() == null) continue;
            Long exist = mapper.selectCount(new LambdaQueryWrapper<PointsExchangeProduct>()
                    .eq(PointsExchangeProduct::getProductId, req.getProductId()));
            if (exist != null && exist > 0) continue;
            req.setId(null);
            req.setTenantId(tenantId());
            if (req.getStatus() == null) req.setStatus(1);
            if (req.getExchangePoints() == null) req.setExchangePoints(BigDecimal.ZERO);
            if (req.getSort() == null) req.setSort(0);
            req.setCreateTime(LocalDateTime.now());
            req.setUpdateTime(LocalDateTime.now());
            n += mapper.insert(req);
        }
        return Result.ok(n);
    }
}
