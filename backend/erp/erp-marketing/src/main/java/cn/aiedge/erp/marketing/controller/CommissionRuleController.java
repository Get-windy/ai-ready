package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.CommissionRule;
import cn.aiedge.erp.marketing.service.CommissionRuleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "佣金规则管理")
@RestController
@RequestMapping("/api/erp/marketing/commission/rule")
@RequiredArgsConstructor
public class CommissionRuleController {

    private final CommissionRuleService commissionRuleService;

    @Operation(summary = "分页查询佣金规则")
    @SaCheckPermission("marketing:commission-rule:list")
    @GetMapping("/page")
    public Result<IPage<CommissionRule>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<CommissionRule> page = new Page<>(pageNum, pageSize);
        return Result.ok(commissionRuleService.page(page));
    }

    @Operation(summary = "查询所有佣金规则")
    @SaCheckPermission("marketing:commission-rule:list")
    @GetMapping("/list")
    public Result<List<CommissionRule>> list() {
        return Result.ok(commissionRuleService.list());
    }

    @Operation(summary = "获取佣金规则详情")
    @SaCheckPermission("marketing:commission-rule:detail")
    @GetMapping("/{id}")
    public Result<CommissionRule> getById(@PathVariable Long id) {
        return Result.ok(commissionRuleService.getById(id));
    }

    @Operation(summary = "新增佣金规则")
    @SaCheckPermission("marketing:commission-rule:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody CommissionRule rule) {
        return Result.ok(commissionRuleService.save(rule));
    }

    @Operation(summary = "更新佣金规则")
    @SaCheckPermission("marketing:commission-rule:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody CommissionRule rule) {
        rule.setId(id);
        return Result.ok(commissionRuleService.updateById(rule));
    }

    @Operation(summary = "删除佣金规则")
    @SaCheckPermission("marketing:commission-rule:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(commissionRuleService.removeById(id));
    }
}
