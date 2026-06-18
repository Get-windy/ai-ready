package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsWarehouse;
import cn.aiedge.wms.warehouse.service.WarehouseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@Tag(name = "仓库管理")
@RestController
@RequestMapping("/api/wms/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "新增仓库")
    @PostMapping("/save")
    public Result<WmsWarehouse> save(@Valid @RequestBody WmsWarehouse warehouse) {
        warehouseService.saveWarehouse(warehouse);
        log.info("新增仓库: id={}, name={}", warehouse.getId(), warehouse.getWarehouseName());
        return Result.ok(warehouse);
    }

    @Operation(summary = "更新仓库")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsWarehouse warehouse) {
        boolean updated = warehouseService.updateWarehouse(warehouse);
        if (updated) {
            log.info("更新仓库: id={}", warehouse.getId());
        }
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询仓库")
    @GetMapping("/{id}")
    public Result<WmsWarehouse> getById(@PathVariable @NotNull(message = "仓库ID不能为空") Long id) {
        WmsWarehouse warehouse = warehouseService.getWarehouseById(id);
        if (warehouse == null) {
            return Result.fail("仓库不存在");
        }
        return Result.ok(warehouse);
    }

    @Operation(summary = "分页查询仓库")
    @GetMapping("/page")
    public Result<Page<WmsWarehouse>> page(@Valid Page<WmsWarehouse> page, WmsWarehouse query) {
        return Result.ok(warehouseService.pageWarehouse(page, query));
    }

    @Operation(summary = "删除仓库")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "仓库ID不能为空") Long id) {
        warehouseService.removeWarehouse(id);
        log.info("删除仓库: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "查询所有仓库")
    @GetMapping("/list-all")
    public Result<List<WmsWarehouse>> listAll() {
        Page<WmsWarehouse> page = warehouseService.pageWarehouse(new Page<>(1, 10000), new WmsWarehouse());
        return Result.ok(page.getRecords());
    }
}
