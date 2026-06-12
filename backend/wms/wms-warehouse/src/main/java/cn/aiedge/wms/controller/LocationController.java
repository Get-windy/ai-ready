package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsLocation;
import cn.aiedge.wms.warehouse.service.WarehouseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Validated
@Tag(name = "货位管理")
@RestController
@RequestMapping("/api/wms/location")
@RequiredArgsConstructor
public class LocationController {

    private final WarehouseService warehouseService;

    @Operation(summary = "新增货位")
    @PostMapping("/save")
    public Result<WmsLocation> save(@Valid @RequestBody WmsLocation location) {
        warehouseService.saveLocation(location);
        log.info("新增货位: id={}, code={}", location.getId(), location.getLocationCode());
        return Result.ok(location);
    }

    @Operation(summary = "更新货位")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsLocation location) {
        boolean updated = warehouseService.updateLocation(location);
        if (updated) {
            log.info("更新货位: id={}", location.getId());
        }
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询货位")
    @GetMapping("/{id}")
    public Result<WmsLocation> getById(@PathVariable @NotNull(message = "货位ID不能为空") Long id) {
        WmsLocation location = warehouseService.getLocationById(id);
        if (location == null) {
            return Result.fail("货位不存在");
        }
        return Result.ok(location);
    }

    @Operation(summary = "分页查询货位")
    @GetMapping("/page")
    public Result<Page<WmsLocation>> page(@Valid Page<WmsLocation> page, WmsLocation query) {
        return Result.ok(warehouseService.pageLocation(page, query));
    }

    @Operation(summary = "删除货位")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "货位ID不能为空") Long id) {
        warehouseService.removeLocation(id);
        log.info("删除货位: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "根据仓库查询货位列表")
    @GetMapping("/list-by-warehouse/{warehouseId}")
    public Result<List<WmsLocation>> listByWarehouse(
            @PathVariable @NotNull(message = "仓库ID不能为空") Long warehouseId) {
        return Result.ok(warehouseService.listByWarehouseId(warehouseId));
    }

    @Operation(summary = "推荐上架货位")
    @GetMapping("/recommend")
    public Result<List<WmsLocation>> recommend(
            @RequestParam @NotNull Long warehouseId,
            @RequestParam @NotNull Long productId,
            @RequestParam @Positive BigDecimal quantity) {
        return Result.ok(warehouseService.recommendLocations(warehouseId, productId, quantity));
    }
}
