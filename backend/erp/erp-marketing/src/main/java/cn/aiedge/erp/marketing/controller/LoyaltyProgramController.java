package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.LoyaltyProgram;
import cn.aiedge.erp.marketing.service.LoyaltyProgramService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
@Tag(name = "促销与忠诚程序管理")
@RestController
@RequestMapping("/api/erp/marketing/program")
@RequiredArgsConstructor
public class LoyaltyProgramController {

    private final LoyaltyProgramService loyaltyProgramService;

    @Operation(summary = "分页查询程序列表")
    @SaCheckPermission("marketing:program:list")
    @GetMapping("/page")
    public Result<IPage<LoyaltyProgram>> page(
            @RequestParam(required = false) String programType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<LoyaltyProgram> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LoyaltyProgram> wrapper = new LambdaQueryWrapper<>();
        if (programType != null && !programType.isEmpty()) {
            wrapper.eq(LoyaltyProgram::getProgramType, programType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(LoyaltyProgram::getName, keyword);
        }
        wrapper.orderByAsc(LoyaltyProgram::getSortOrder);
        return Result.ok(loyaltyProgramService.page(page, wrapper));
    }

    @Operation(summary = "按类型查询程序列表")
    @SaCheckPermission("marketing:program:list")
    @GetMapping("/list")
    public Result<List<LoyaltyProgram>> list(@RequestParam(required = false) String programType) {
        if (programType != null && !programType.isEmpty()) {
            return Result.ok(loyaltyProgramService.listByType(programType));
        }
        return Result.ok(loyaltyProgramService.list());
    }

    @Operation(summary = "查询当前有效的促销程序")
    @SaCheckPermission("marketing:program:view")
    @GetMapping("/active")
    public Result<List<LoyaltyProgram>> active() {
        return Result.ok(loyaltyProgramService.listActive());
    }

    @Operation(summary = "获取程序详情")
    @SaCheckPermission("marketing:program:detail")
    @GetMapping("/{id}")
    public Result<LoyaltyProgram> getById(@PathVariable Long id) {
        LoyaltyProgram program = loyaltyProgramService.getById(id);
        if (program == null) {
            return Result.fail("程序不存在");
        }
        return Result.ok(program);
    }

    @Operation(summary = "创建程序")
    @SaCheckPermission("marketing:program:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody LoyaltyProgram program) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        program.setTenantId(tenantId);
        program.setId(null);
        if (program.getIsActive() == null) program.setIsActive(1);
        if (program.getUsageCount() == null) program.setUsageCount(0);
        return Result.ok(loyaltyProgramService.save(program));
    }

    @Operation(summary = "更新程序")
    @SaCheckPermission("marketing:program:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody LoyaltyProgram program) {
        program.setId(id);
        return Result.ok(loyaltyProgramService.updateById(program));
    }

    @Operation(summary = "删除程序")
    @SaCheckPermission("marketing:program:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(loyaltyProgramService.removeById(id));
    }
}
