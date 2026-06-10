package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockDamage;
import cn.aiedge.erp.stock.entity.StockDamageItem;
import cn.aiedge.erp.stock.service.StockDamageService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/damage")
@RequiredArgsConstructor
@Tag(name = "报损管理", description = "报损单创建、审批、出库等操作")
public class StockDamageController {

    private final StockDamageService damageService;

    @lombok.Data
    public static class CreateDamageRequest {
        private LocalDate damageDate;
        private Long warehouseId;
        private Long locationId;
        private Integer damageCause;
        private String remark;
        private List<StockDamageItem> items;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询报损单")
    public Result<Page<StockDamage>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "报损原因") @RequestParam(required = false) Integer damageCause,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(damageService.pageList(keyword, warehouseId, status, damageCause, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取报损单详情")
    public Result<StockDamage> getById(@PathVariable Long id) {
        StockDamage d = damageService.getById(id);
        if (d == null) throw BusinessException.notFound("报损单不存在");
        return Result.ok(d);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取报损明细")
    public Result<List<StockDamageItem>> getItems(@PathVariable Long id) {
        return Result.ok(damageService.getItems(id));
    }

    @PostMapping
    @Operation(summary = "创建报损单")
    public Result<StockDamage> create(@RequestBody CreateDamageRequest request) {
        StockDamage damage = new StockDamage();
        damage.setTenantId(1L);
        damage.setDamageDate(request.getDamageDate());
        damage.setWarehouseId(request.getWarehouseId());
        damage.setLocationId(request.getLocationId());
        damage.setDamageCause(request.getDamageCause());
        damage.setRemark(request.getRemark());
        return Result.ok(damageService.createDamage(damage, request.getItems()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public Result<StockDamage> submit(@PathVariable Long id) {
        return Result.ok(damageService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<StockDamage> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        return Result.ok(damageService.approve(id, StpUtil.getLoginIdAsLong(), note));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<StockDamage> reject(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(damageService.reject(id, reason));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行出库")
    public Result<StockDamage> execute(@PathVariable Long id) {
        return Result.ok(damageService.execute(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消报损单")
    public Result<StockDamage> cancel(@PathVariable Long id, @RequestParam String reason) {
        return Result.ok(damageService.cancel(id, reason));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        return Result.ok(damageService.removeBatchByIds(ids));
    }
}
