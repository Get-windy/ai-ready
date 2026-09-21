package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.AddonRule;
import cn.aiedge.erp.marketing.service.AddonRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "加价购规则管理")
@RestController
@RequestMapping("/api/erp/marketing/addon-rule")
@RequiredArgsConstructor
public class AddonRuleController {

    private final AddonRuleService addonRuleService;

    @Operation(summary = "分页查询加价购规则")
    @SaCheckPermission("marketing:addon-rule:list")
    @GetMapping("/page")
    public Result<IPage<AddonRule>> page(
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<AddonRule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AddonRule> wrapper = new LambdaQueryWrapper<>();
        if (ruleName != null && !ruleName.isEmpty()) {
            wrapper.like(AddonRule::getRuleName, ruleName);
        }
        if (status != null) {
            wrapper.eq(AddonRule::getStatus, status);
        }
        wrapper.orderByAsc(AddonRule::getSort).orderByDesc(AddonRule::getCreateTime);
        return Result.ok(addonRuleService.page(page, wrapper));
    }

    @Operation(summary = "查询加价购规则详情")
    @SaCheckPermission("marketing:addon-rule:detail")
    @GetMapping("/{id}")
    public Result<AddonRule> getById(@PathVariable Long id) {
        return Result.ok(addonRuleService.getById(id));
    }

    @Operation(summary = "创建加价购规则")
    @SaCheckPermission("marketing:addon-rule:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody AddonRule addonRule) {
        addonRule.setId(null);
        if (addonRule.getStatus() == null) addonRule.setStatus(AddonRule.STATUS_DISABLED);
        addonRule.setCreatorName(cn.aiedge.base.utils.SecurityUtils.getCurrentUsername());
        addonRule.setCreateTime(LocalDateTime.now());
        addonRule.setUpdateTime(LocalDateTime.now());
        return Result.ok(addonRuleService.save(addonRule));
    }

    @Operation(summary = "更新加价购规则")
    @SaCheckPermission("marketing:addon-rule:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody AddonRule addonRule) {
        addonRule.setId(id);
        return Result.ok(addonRuleService.updateById(addonRule));
    }

    @Operation(summary = "删除加价购规则")
    @SaCheckPermission("marketing:addon-rule:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(addonRuleService.removeById(id));
    }

    @Operation(summary = "启用加价购规则")
    @SaCheckPermission("marketing:addon-rule:create")
    @PostMapping("/{id}/enable")
    public Result<Boolean> enable(@PathVariable Long id) {
        addonRuleService.enable(id);
        return Result.ok(true);
    }

    @Operation(summary = "停用加价购规则")
    @SaCheckPermission("marketing:addon-rule:create")
    @PostMapping("/{id}/disable")
    public Result<Boolean> disable(@PathVariable Long id) {
        addonRuleService.disable(id);
        return Result.ok(true);
    }
}
