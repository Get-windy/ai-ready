package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.MarketingRule;
import cn.aiedge.erp.marketing.service.MarketingRuleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "营销规则管理")
@RestController
@RequestMapping("/api/erp/marketing/rules")
@RequiredArgsConstructor
public class MarketingRuleController {

    private final MarketingRuleService ruleService;

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

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(ruleService.removeById(id));
    }
}
