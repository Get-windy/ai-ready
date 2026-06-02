package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会计科目Controller
 */
@Tag(name = "会计科目管理", description = "会计科目CRUD及树形结构接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/subject")
@RequiredArgsConstructor
public class AccountSubjectController {

    private final AccountSubjectService accountSubjectService;

    @Operation(summary = "查询会计科目列表")
    @GetMapping("/list")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/list', 'finance:subject:view')")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "查询会计科目列表")
    public Result<List<AccountSubjectDTO>> list() {
        return Result.success(accountSubjectService.getAll());
    }

    @Operation(summary = "查询会计科目树形结构")
    @GetMapping("/tree")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/list', 'finance:subject:view')")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "查询会计科目树形结构")
    public Result<List<AccountSubjectDTO>> tree() {
        return Result.success(accountSubjectService.getTree());
    }

    @Operation(summary = "根据ID查询会计科目")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/view', 'finance:subject:view')")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "根据ID查询会计科目")
    public Result<AccountSubjectDTO> getById(@Parameter(description = "科目ID") @PathVariable Long id) {
        return Result.success(accountSubjectService.getById(id));
    }

    @Operation(summary = "根据科目类型查询")
    @GetMapping("/type/{subjectType}")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/list', 'finance:subject:view')")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "根据科目类型查询")
    public Result<List<AccountSubjectDTO>> getByType(@Parameter(description = "科目类型(1-资产,2-负债,3-权益,4-成本,5-损益)") @PathVariable Integer subjectType) {
        return Result.success(accountSubjectService.getByType(subjectType));
    }

    @Operation(summary = "创建会计科目")
    @PostMapping("/")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/create', 'finance:subject:create')")
    @OperationLog(module = "会计科目管理", type = "CREATE", desc = "创建会计科目")
    public Result<AccountSubjectDTO> create(@RequestBody AccountSubjectDTO dto) {
        AccountSubjectDTO result = accountSubjectService.create(dto);
        return Result.success("创建成功", result);
    }

    @Operation(summary = "更新会计科目")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/update', 'finance:subject:edit')")
    @OperationLog(module = "会计科目管理", type = "UPDATE", desc = "更新会计科目")
    public Result<AccountSubjectDTO> update(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @RequestBody AccountSubjectDTO dto) {
        AccountSubjectDTO result = accountSubjectService.update(id, dto);
        return Result.success("更新成功", result);
    }

    @Operation(summary = "删除会计科目")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/delete', 'finance:subject:delete')")
    @OperationLog(module = "会计科目管理", type = "DELETE", desc = "删除会计科目")
    public Result<Void> delete(@Parameter(description = "科目ID") @PathVariable Long id) {
        accountSubjectService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/停用会计科目")
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasPermission('/api/erp/finance/subject/edit', 'finance:subject:edit')")
    @OperationLog(module = "会计科目管理", type = "UPDATE", desc = "启用/停用会计科目")
    public Result<AccountSubjectDTO> toggleEnabled(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        AccountSubjectDTO result = accountSubjectService.enable(id, enabled);
        return Result.success("操作成功", result);
    }
}
