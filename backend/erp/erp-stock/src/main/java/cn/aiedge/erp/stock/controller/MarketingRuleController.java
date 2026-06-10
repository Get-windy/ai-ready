package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.MarketingRule;
import cn.aiedge.erp.stock.entity.MarketingRuleProduct;
import cn.aiedge.erp.stock.entity.MarketingRulePartner;
import cn.aiedge.erp.stock.mapper.MarketingRuleProductMapper;
import cn.aiedge.erp.stock.mapper.MarketingRulePartnerMapper;
import cn.aiedge.erp.stock.service.MarketingRuleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "营销规则管理")
@RestController
@RequestMapping("/api/erp/marketing/rules")
@RequiredArgsConstructor
public class MarketingRuleController {

    private final MarketingRuleService ruleService;
    private final MarketingRuleProductMapper ruleProductMapper;
    private final MarketingRulePartnerMapper rulePartnerMapper;

    @Operation(summary = "分页查询规则")
    @GetMapping("/page")
    public Result<IPage<MarketingRule>> page(
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<MarketingRule> page = new Page<>(pageNum, pageSize);
        QueryWrapper<MarketingRule> wrapper = new QueryWrapper<MarketingRule>()
                .eq("deleted", 0);
        if (ruleType != null) wrapper.eq("rule_type", ruleType);
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("create_time");
        return Result.ok(ruleService.page(page, wrapper));
    }

    @Operation(summary = "查询规则详情")
    @GetMapping("/{id}")
    public Result<MarketingRule> getById(@PathVariable Long id) {
        return Result.ok(ruleService.getById(id));
    }

    @Operation(summary = "新增规则")
    @PostMapping
    public Result<Boolean> create(@RequestBody MarketingRule rule) {
        return Result.ok(ruleService.save(rule));
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody MarketingRule rule) {
        rule.setId(id);
        return Result.ok(ruleService.updateById(rule));
    }

    @Operation(summary = "更新规则状态")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        MarketingRule rule = new MarketingRule();
        rule.setId(id);
        rule.setStatus(status);
        return Result.ok(ruleService.updateById(rule));
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(ruleService.removeById(id));
    }

    @Operation(summary = "查询规则适用产品")
    @GetMapping("/{id}/products")
    public Result<List<MarketingRuleProduct>> getProducts(@PathVariable Long id) {
        return Result.ok(ruleProductMapper.selectList(
                new QueryWrapper<MarketingRuleProduct>().eq("rule_id", id).eq("deleted", 0)));
    }

    @Operation(summary = "设置规则适用产品")
    @PostMapping("/{id}/products")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> setProducts(@PathVariable Long id, @RequestBody List<MarketingRuleProduct> products) {
        ruleProductMapper.delete(new QueryWrapper<MarketingRuleProduct>().eq("rule_id", id));
        if (products.isEmpty()) return Result.ok(true);
        products.forEach(p -> p.setRuleId(id));
        products.forEach(ruleProductMapper::insert);
        return Result.ok(true);
    }

    @Operation(summary = "查询规则适用客户")
    @GetMapping("/{id}/partners")
    public Result<List<MarketingRulePartner>> getPartners(@PathVariable Long id) {
        return Result.ok(rulePartnerMapper.selectList(
                new QueryWrapper<MarketingRulePartner>().eq("rule_id", id).eq("deleted", 0)));
    }

    @Operation(summary = "设置规则适用客户")
    @PostMapping("/{id}/partners")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> setPartners(@PathVariable Long id, @RequestBody List<MarketingRulePartner> partners) {
        rulePartnerMapper.delete(new QueryWrapper<MarketingRulePartner>().eq("rule_id", id));
        if (partners.isEmpty()) return Result.ok(true);
        partners.forEach(p -> p.setRuleId(id));
        partners.forEach(rulePartnerMapper::insert);
        return Result.ok(true);
    }
}
