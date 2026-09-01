package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.StockDamageItemVO;
import cn.aiedge.erp.stock.dto.StockDamageQuery;
import cn.aiedge.erp.stock.entity.StockDamage;
import cn.aiedge.erp.stock.entity.StockDamageItem;
import cn.aiedge.erp.stock.service.StockDamageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/damage")
@RequiredArgsConstructor
@Tag(name = "报损单管理", description = "报损单创建、送审、记账、出库等操作")
public class StockDamageController {

    private final StockDamageService damageService;

    @GetMapping("/page")
    @Operation(summary = "分页查询报损单(按单据)")
    public Result<Page<StockDamage>> page(@org.springframework.web.bind.annotation.ModelAttribute StockDamageQuery query) {
        return Result.ok(damageService.pageList(query));
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询报损明细(按明细)")
    public Result<Page<StockDamageItemVO>> pageDetail(@org.springframework.web.bind.annotation.ModelAttribute StockDamageQuery query) {
        return Result.ok(damageService.pageDetail(query));
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一报损单号")
    public Result<String> nextNo() {
        return Result.ok(damageService.generateNo());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取报损单详情")
    public Result<StockDamage> getById(@PathVariable Long id) {
        return Result.ok(damageService.getDetail(id));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取报损明细")
    public Result<List<StockDamageItem>> getItems(@PathVariable Long id) {
        return Result.ok(damageService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建报损单(保存草稿)")
    public Result<StockDamage> create(@RequestBody StockDamage damage) {
        List<StockDamageItem> items = damage.getItems();
        damage.setItems(null);
        return Result.ok(damageService.createStockDamage(damage, items));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报损单(草稿)")
    public Result<StockDamage> update(@PathVariable Long id, @RequestBody StockDamage damage) {
        List<StockDamageItem> items = damage.getItems();
        damage.setItems(null);
        return Result.ok(damageService.updateStockDamage(id, damage, items));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报损单")
    public Result<Boolean> delete(@PathVariable Long id) {
        StockDamage d = damageService.getById(id);
        if (d != null && d.getStatus() != 0) {
            return Result.fail("只有草稿状态的报损单可以删除");
        }
        return Result.ok(damageService.removeById(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "执行记帐(出库生效)")
    public Result<StockDamage> complete(@PathVariable Long id) {
        return Result.ok(damageService.execute(id));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行记帐(出库生效)")
    public Result<StockDamage> execute(@PathVariable Long id) {
        return Result.ok(damageService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消报损单")
    public Result<StockDamage> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return Result.ok(damageService.cancel(id, reason));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除报损单")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            StockDamage d = damageService.getById(id);
            if (d != null && d.getStatus() != 0) {
                return Result.fail("存在非草稿状态的报损单，不能批量删除");
            }
        }
        return Result.ok(damageService.removeBatchByIds(ids));
    }
}
