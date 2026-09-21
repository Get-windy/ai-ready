package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.ProductPointsRule;
import cn.aiedge.erp.marketing.mapper.ProductPointsRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 商品级积分系数（会员设置「详细设置」入口，菜单 80302）
 * ⚠️ 本系统建模：对标弹窗明细未实测。
 */
@Slf4j
@Tag(name = "商品级积分系数")
@RestController
@RequestMapping("/api/erp/marketing/product-points-rule")
@RequiredArgsConstructor
public class ProductPointsRuleController {

    private final ProductPointsRuleMapper mapper;

    @Operation(summary = "查询商品级积分系数列表")
    @SaCheckPermission("marketing:product-points-rule:list")
    @GetMapping("/list")
    public Result<List<ProductPointsRule>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ProductPointsRule> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ProductPointsRule::getProductCode, keyword)
                    .or().like(ProductPointsRule::getProductName, keyword));
        }
        wrapper.orderByDesc(ProductPointsRule::getId);
        return Result.ok(mapper.selectList(wrapper));
    }

    @Operation(summary = "新增商品级积分系数")
    @SaCheckPermission("marketing:product-points-rule:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductPointsRule rule) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        rule.setId(null);
        rule.setTenantId(tenantId == null ? 1L : tenantId);
        if (rule.getStatus() == null) rule.setStatus(1);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        return Result.ok(mapper.insert(rule) > 0);
    }

    @Operation(summary = "更新商品级积分系数")
    @SaCheckPermission("marketing:product-points-rule:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductPointsRule rule) {
        rule.setId(id);
        rule.setTenantId(null);
        rule.setUpdateTime(LocalDateTime.now());
        return Result.ok(mapper.updateById(rule) > 0);
    }

    @Operation(summary = "删除商品级积分系数")
    @SaCheckPermission("marketing:product-points-rule:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(mapper.deleteById(id) > 0);
    }
}
