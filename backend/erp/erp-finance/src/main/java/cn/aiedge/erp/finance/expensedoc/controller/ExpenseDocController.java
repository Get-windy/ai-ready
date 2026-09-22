package cn.aiedge.erp.finance.expensedoc.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocItemVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocSaveDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocVO;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseDoc;
import cn.aiedge.erp.finance.expensedoc.service.ExpenseDocService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 费用单 Controller
 * 非主营支出费用登记：往来单位费用/内部费用记账生成会计凭证（KJPZ-）。
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/expense-doc")
@RequiredArgsConstructor
@Tag(name = "费用单管理", description = "费用单：非主营支出费用登记，往来单位费用/内部费用，记账生成凭证")
public class ExpenseDocController {

    private final ExpenseDocService expenseDocService;

    @Operation(summary = "多条件分页查询费用单(按单据)")
    @SaCheckPermission("finance:expense-doc:list")
    @GetMapping("/page")
    public Result<Page<ExpenseDoc>> page(ExpenseDocQuery query) {
        return Result.ok(expenseDocService.pageQuery(query));
    }

    @Operation(summary = "多条件分页查询费用单(按单据，别名)")
    @SaCheckPermission("finance:expense-doc:list")
    @GetMapping("/doc-query")
    public Result<Page<ExpenseDoc>> docQuery(ExpenseDocQuery query) {
        return Result.ok(expenseDocService.pageQuery(query));
    }

    @Operation(summary = "分页查询费用明细(按明细)")
    @SaCheckPermission("finance:expense-doc:view")
    @GetMapping("/page-detail")
    public Result<Page<ExpenseDocItemVO>> pageDetail(ExpenseDocQuery query) {
        return Result.ok(expenseDocService.pageDetail(query));
    }

    @Operation(summary = "生成下一费用单号")
    @SaCheckPermission("finance:expense-doc:list")
    @GetMapping("/next-no")
    public Result<String> nextNo(@RequestParam(required = false) String prefix) {
        return Result.ok(expenseDocService.generateDocNo());
    }

    @Operation(summary = "查询费用单详情（含费用项明细细）")
    @SaCheckPermission("finance:expense-doc:detail")
    @GetMapping("/{id}")
    public Result<ExpenseDocVO> getById(@PathVariable Long id) {
        return Result.ok(expenseDocService.getDetail(id));
    }

    @Operation(summary = "保存费用单草稿（含费用项明细）")
    @SaCheckPermission("finance:expense-doc:create")
    @PostMapping("/create")
    public Result<ExpenseDoc> create(@Valid @RequestBody ExpenseDocSaveDTO dto) {
        return Result.ok(expenseDocService.saveDraft(dto));
    }

    @Operation(summary = "更新费用单草稿（明细整体替换）")
    @SaCheckPermission("finance:expense-doc:create")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody ExpenseDocSaveDTO dto) {
        expenseDocService.update(dto);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "费用单记账（生成凭证 + 动账户余额 + 记资金流水）")
    @SaCheckPermission("finance:expense-doc:confirm")
    @PostMapping("/confirm")
    public Result<ExpenseDoc> confirm(@Parameter(description = "单据ID") @RequestParam Long id,
                                      @Parameter(description = "记账人ID") @RequestParam(required = false) Long operatorId,
                                      @Parameter(description = "记账人") @RequestParam(required = false) String operatorName) {
        ExpenseDoc doc = expenseDocService.confirm(id, operatorId, operatorName);
        return Result.ok(doc);
    }

    @Operation(summary = "取消费用单（仅草稿）")
    @SaCheckPermission("finance:expense-doc:cancel")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam Long id) {
        expenseDocService.cancel(id);
        return Result.ok("取消成功");
    }

    @Operation(summary = "删除费用单（仅草稿/已取消）")
    @SaCheckPermission("finance:expense-doc:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        expenseDocService.remove(id);
        return Result.ok("删除成功");
    }
}
