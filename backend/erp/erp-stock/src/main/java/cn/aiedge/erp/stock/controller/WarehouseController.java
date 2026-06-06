package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仓库Controller
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "仓库管理", description = "仓库查询接口")
@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "查询仓库列表")
    @GetMapping("/list")
    public Result<List<Warehouse>> list() {
        List<Warehouse> list = warehouseService.getWarehouseList();
        return Result.ok(list);
    }
}
