package cn.aiedge.erp.finance.cashtransfer.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferItemVO;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferQuery;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferSaveDTO;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferVO;
import cn.aiedge.erp.finance.cashtransfer.entity.CashTransfer;
import cn.aiedge.erp.finance.cashtransfer.service.CashTransferService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 提存（提存现金转账）Controller
 * 资金在企业账户间移动，不涉及往来单位。记账生成会计凭证（KJPZ-）。
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/cash-transfer")
@RequiredArgsConstructor
@Tag(name = "提存管理", description = "提存/现金转账：资金在企业账户间移动，记账生成凭证")
public class CashTransferController {

    private final CashTransferService cashTransferService;

    @Operation(summary = "多条件分页查询提存单(按单据)")
    @GetMapping("/page")
    public Result<Page<CashTransfer>> page(CashTransferQuery query) {
        return Result.ok(cashTransferService.pageQuery(query));
    }

    @Operation(summary = "多条件分页查询提存单(按单据，别名)")
    @GetMapping("/doc-query")
    public Result<Page<CashTransfer>> docQuery(CashTransferQuery query) {
        return Result.ok(cashTransferService.pageQuery(query));
    }

    @Operation(summary = "分页查询提存明细(按明细)")
    @GetMapping("/page-detail")
    public Result<Page<CashTransferItemVO>> pageDetail(CashTransferQuery query) {
        return Result.ok(cashTransferService.pageDetail(query));
    }

    @Operation(summary = "生成下一提存单号")
    @GetMapping("/next-no")
    public Result<String> nextNo(@RequestParam(required = false) String prefix) {
        return Result.ok(cashTransferService.generateDocNo());
    }

    @Operation(summary = "查询提存单详情（含转入账户明细）")
    @GetMapping("/{id}")
    public Result<CashTransferVO> getById(@PathVariable Long id) {
        return Result.ok(cashTransferService.getDetail(id));
    }

    @Operation(summary = "保存提存单草稿（含转入账户明细）")
    @PostMapping("/create")
    public Result<CashTransfer> create(@Valid @RequestBody CashTransferSaveDTO dto) {
        return Result.ok(cashTransferService.saveDraft(dto));
    }

    @Operation(summary = "更新提存单草稿（明细整体替换）")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody CashTransferSaveDTO dto) {
        cashTransferService.update(dto);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "提存单记账（生成凭证 + 动账户余额 + 记资金流水）")
    @PostMapping("/confirm")
    public Result<CashTransfer> confirm(@Parameter(description = "单据ID") @RequestParam Long id,
                                        @Parameter(description = "记账人ID") @RequestParam(required = false) Long operatorId,
                                        @Parameter(description = "记账人") @RequestParam(required = false) String operatorName) {
        CashTransfer transfer = cashTransferService.confirm(id, operatorId, operatorName);
        return Result.ok(transfer);
    }

    @Operation(summary = "取消提存单（仅草稿）")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam Long id) {
        cashTransferService.cancel(id);
        return Result.ok("取消成功");
    }

    @Operation(summary = "删除提存单（仅草稿/已取消）")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        cashTransferService.remove(id);
        return Result.ok("删除成功");
    }
}
