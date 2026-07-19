package cn.aiedge.erp.stock.controller.initial;

import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.service.StockService;
import cn.aiedge.common.result.ApiResponse;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 期初库存管理Controller
 * 用于管理系统初始库存数据
 */
@Slf4j
@Tag(name = "期初库存管理", description = "期初库存数据的管理")
@RestController
@RequestMapping("/api/set/initial-stock")
@RequiredArgsConstructor
public class InitialStockController {

    private final StockService stockService;

    @GetMapping("/page")
    @Operation(summary = "分页查询期初库存")
    @SaCheckLogin
    public ApiResponse<Page<InitialStockVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName) {

        Page<Stock> stockPage = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getIsInitial, 1); // 只查询期初库存记录

        if (productCode != null && !productCode.trim().isEmpty()) {
            queryWrapper.like(Stock::getProductCode, productCode);
        }
        if (productName != null && !productName.trim().isEmpty()) {
            queryWrapper.like(Stock::getProductName, productName);
        }

        Page<Stock> pageResult = stockService.page(stockPage, queryWrapper);

        Page<InitialStockVO> voPage = new Page<>();
        voPage.setCurrent(pageResult.getCurrent());
        voPage.setSize(pageResult.getSize());
        voPage.setTotal(pageResult.getTotal());

        List<InitialStockVO> voList = pageResult.getRecords().stream()
                .map(this::convertToVO)
                .collect(java.util.stream.Collectors.toList());

        voPage.setRecords(voList);

        return ApiResponse.ok(voPage);
    }

    @PostMapping("/save")
    @Operation(summary = "保存期初库存")
    @SaCheckLogin
    public ApiResponse<Void> save(@RequestBody List<InitialStockDTO> dtoList) {
        // 保存期初库存数据
        stockService.saveInitialStock(dtoList);
        return ApiResponse.ok(null);
    }

    @PutMapping("/update")
    @Operation(summary = "更新期初库存")
    @SaCheckLogin
    public ApiResponse<Void> update(@RequestBody InitialStockDTO dto) {
        // 更新期初库存数据
        stockService.updateInitialStock(dto);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export")
    @Operation(summary = "导出期初库存")
    @SaCheckLogin
    public ApiResponse<List<InitialStockVO>> export(
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName) {
        // 导出期初库存数据
        LambdaQueryWrapper<Stock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Stock::getIsInitial, 1);

        if (productCode != null && !productCode.trim().isEmpty()) {
            queryWrapper.like(Stock::getProductCode, productCode);
        }
        if (productName != null && !productName.trim().isEmpty()) {
            queryWrapper.like(Stock::getProductName, productName);
        }

        List<Stock> stockList = stockService.list(queryWrapper);
        List<InitialStockVO> voList = stockList.stream()
                .map(this::convertToVO)
                .collect(java.util.stream.Collectors.toList());

        return ApiResponse.ok(voList);
    }

    private InitialStockVO convertToVO(Stock stock) {
        InitialStockVO vo = new InitialStockVO();
        vo.setId(stock.getId());
        vo.setProductCode(stock.getProductCode());
        vo.setProductName(stock.getProductName());
        vo.setWarehouseName(stock.getWarehouseName());
        vo.setQuantity(stock.getQuantity());
        vo.setUnitPrice(stock.getUnitPrice());
        vo.setCreateTime(stock.getCreateTime());
        return vo;
    }
}