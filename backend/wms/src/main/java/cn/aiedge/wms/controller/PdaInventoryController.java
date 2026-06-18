package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.inventory.service.InventoryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse/inventory")
@Tag(name = "PDA-库存")
@RequiredArgsConstructor
public class PdaInventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "查询库存（按商品编码或货位编码）")
    @GetMapping("/query")
    public Result<List<WmsInventory>> query(
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String locationCode) {
        WmsInventory query = new WmsInventory();
        query.setProductCode(productCode);
        query.setLocationCode(locationCode);
        Page<WmsInventory> page = inventoryService.pageInventory(
                new Page<>(1, 100), query);
        log.info("PDA库存查询: productCode={}, locationCode={}, results={}",
                productCode, locationCode, page.getRecords().size());
        return Result.ok(page.getRecords());
    }
}
