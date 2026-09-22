package cn.aiedge.erp.finance.arapadjust.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustQuery;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustSaveDTO;
import cn.aiedge.erp.finance.arapadjust.dto.ArApAdjustVO;
import cn.aiedge.erp.finance.arapadjust.entity.ArApAdjust;
import cn.aiedge.erp.finance.arapadjust.service.ArApAdjustService;
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
 * 应收应付调整 Controller
 * 不动资金账户的往来余额调整：应收增加/应收减少/应付增加/应付减少四向。
 * 记账生成会计凭证（KJPZ-），并调整应收/应付余额。
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/ar-ap-adjust")
@RequiredArgsConstructor
@Tag(name = "应收应付调整", description = "应收应付调整：不动资金账户的往来余额调整，记账经凭证")
public class ArApAdjustController {

    private final ArApAdjustService arApAdjustService;

    @Operation(summary = "多条件分页查询调整单(单表)")
    @SaCheckPermission("finance:ar-ap-adjust:list")
    @GetMapping("/page")
    public Result<Page<ArApAdjust>> page(ArApAdjustQuery query) {
        return Result.ok(arApAdjustService.pageQuery(query));
    }

    @Operation(summary = "多条件分页查询调整单(单表，别名)")
    @SaCheckPermission("finance:ar-ap-adjust:list")
    @GetMapping("/doc-query")
    public Result<Page<ArApAdjust>> docQuery(ArApAdjustQuery query) {
        return Result.ok(arApAdjustService.pageQuery(query));
    }

    @Operation(summary = "生成下一调整单号")
    @SaCheckPermission("finance:ar-ap-adjust:list")
    @GetMapping("/next-no")
    public Result<String> nextNo(@RequestParam(required = false) String prefix) {
        return Result.ok(arApAdjustService.generateDocNo());
    }

    @Operation(summary = "查询调整单详情（含科目明细）")
    @SaCheckPermission("finance:ar-ap-adjust:detail")
    @GetMapping("/{id}")
    public Result<ArApAdjustVO> getById(@PathVariable Long id) {
        return Result.ok(arApAdjustService.getDetail(id));
    }

    @Operation(summary = "保存调整单草稿（含科目明细）")
    @SaCheckPermission("finance:ar-ap-adjust:create")
    @PostMapping("/create")
    public Result<ArApAdjust> create(@Valid @RequestBody ArApAdjustSaveDTO dto) {
        return Result.ok(arApAdjustService.saveDraft(dto));
    }

    @Operation(summary = "更新调整单草稿（明细整体替换）")
    @SaCheckPermission("finance:ar-ap-adjust:create")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody ArApAdjustSaveDTO dto) {
        arApAdjustService.update(dto);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "调整单记账（生成凭证 + 调整应收/应付余额）")
    @SaCheckPermission("finance:ar-ap-adjust:confirm")
    @PostMapping("/confirm")
    public Result<ArApAdjust> confirm(@Parameter(description = "单据ID") @RequestParam Long id,
                                      @Parameter(description = "记账人ID") @RequestParam(required = false) Long operatorId,
                                      @Parameter(description = "记账人") @RequestParam(required = false) String operatorName) {
        ArApAdjust adjust = arApAdjustService.confirm(id, operatorId, operatorName);
        return Result.ok(adjust);
    }

    @Operation(summary = "取消调整单（仅草稿）")
    @SaCheckPermission("finance:ar-ap-adjust:cancel")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam Long id) {
        arApAdjustService.cancel(id);
        return Result.ok("取消成功");
    }

    @Operation(summary = "删除调整单（仅草稿/已取消）")
    @SaCheckPermission("finance:ar-ap-adjust:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        arApAdjustService.remove(id);
        return Result.ok("删除成功");
    }
}
