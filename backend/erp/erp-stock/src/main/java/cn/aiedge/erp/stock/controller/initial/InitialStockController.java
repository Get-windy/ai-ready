package cn.aiedge.erp.stock.controller.initial;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.stock.mapper.InitialStockQueryMapper;
import cn.aiedge.erp.stock.service.StockService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 期初库存管理 Controller（设置 → 数据录入 → 库存期初，菜单 70550）
 * <p>
 * 本页是 {@code erp_stock} 中 {@code is_initial = 1} 行的唯一维护入口：开账前录入每个商品在每个仓库的
 * 期初数量与期初成本单价。⚠️ 期初录入**不走库存事件服务**，因此不产生库存流水（设计取舍）。
 * <p>
 * 接口契约（与前端 {@code views/set/initial-stock/index.vue} 严格一一对应，2026-09-18 对齐）：
 * <ul>
 *   <li>{@code GET  /page}          分页查询</li>
 *   <li>{@code POST /save}          新增（body 为数组，沿用批量口径，前端单条也包成数组）</li>
 *   <li>{@code PUT  /update}        编辑（id 在 body）</li>
 *   <li>{@code DELETE /{id}}        删除</li>
 *   <li>{@code GET  /export}        导出全量（与 /page 同口径，前端据此生成 xlsx）</li>
 * </ul>
 * ⚠️ 历史缺陷（2026-09-18 修复）：前端原先调 {@code POST /}、{@code PUT /{id}}、{@code DELETE /{id}} 三个
 * **后端不存在**的路径 → 写操作 100% 404。现统一为「新增/编辑沿用 /save + /update（RPC 口径），
 * 删除补 {@code DELETE /{id}}」—— 二选一不并存，见《库存期初开发文档》§9.2 P0 建议 ①。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "期初库存管理", description = "期初库存数据的管理")
@RestController
@RequestMapping("/api/set/initial-stock")
@RequiredArgsConstructor
public class InitialStockController {

    private final StockService stockService;
    private final InitialStockQueryMapper initialStockQueryMapper;

    @SaCheckPermission("set:initial-stock:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询期初库存")
    @SaCheckLogin
    public ApiResponse<IPage<InitialStockVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String initialQtyFilter) {

        // 交回 Mapper 的返回值（MyBatis-Plus 会把 records/total 填进传入的 Page 并原样返回）
        IPage<InitialStockVO> pageResult = initialStockQueryMapper.selectInitialStockPage(
                new Page<>(pageNum, pageSize), MyBatisPlusConfig.getCurrentTenantIdValue(),
                warehouseId, categoryId, keyword, productCode, productName, initialQtyFilter);
        return ApiResponse.ok(pageResult);
    }

    @SaCheckPermission("set:initial-stock:create")
    @PostMapping("/save")
    @Operation(summary = "保存期初库存")
    @SaCheckLogin
    public ApiResponse<Void> save(@RequestBody List<InitialStockDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return ApiResponse.badRequest("期初库存数据不能为空");
        }
        // 逐条校验：商品与仓库必须能定位（否则数据无法归属，列表按商品/仓库口径也查不出来）
        for (InitialStockDTO dto : dtoList) {
            if (dto.getProductId() == null) {
                return ApiResponse.badRequest("请选择商品");
            }
            if (dto.getWarehouseId() == null) {
                return ApiResponse.badRequest("请选择仓库");
            }
        }
        stockService.saveInitialStock(dtoList);
        return ApiResponse.ok(null);
    }

    @SaCheckPermission("set:initial-stock:update")
    @PutMapping("/update")
    @Operation(summary = "更新期初库存")
    @SaCheckLogin
    public ApiResponse<Void> update(@RequestBody InitialStockDTO dto) {
        if (dto == null || dto.getId() == null) {
            return ApiResponse.badRequest("期初库存ID不能为空");
        }
        stockService.updateInitialStock(dto);
        return ApiResponse.ok(null);
    }

    @SaCheckPermission("set:initial-stock:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除期初库存")
    @SaCheckLogin
    public ApiResponse<Void> delete(@PathVariable Long id) {
        boolean removed = stockService.deleteInitialStock(id);
        if (!removed) {
            return ApiResponse.badRequest("期初库存记录不存在或不允许删除");
        }
        return ApiResponse.ok(null);
    }

    @SaCheckPermission("set:initial-stock:export")
    @GetMapping("/export")
    @Operation(summary = "导出期初库存")
    @SaCheckLogin
    public ApiResponse<List<InitialStockVO>> export(
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String initialQtyFilter) {

        List<InitialStockVO> voList = initialStockQueryMapper.selectInitialStockList(
                MyBatisPlusConfig.getCurrentTenantIdValue(),
                warehouseId, categoryId, keyword, productCode, productName, initialQtyFilter);
        return ApiResponse.ok(voList);
    }
}
