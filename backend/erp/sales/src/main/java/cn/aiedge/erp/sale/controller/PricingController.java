package cn.aiedge.erp.sale.controller;

import cn.aiedge.erp.sale.dto.PriceCalculationRequest;
import cn.aiedge.erp.sale.dto.PriceCalculationResult;
import cn.aiedge.erp.sale.dto.PriceStrategyDTO;
import cn.aiedge.erp.sale.service.IPriceCalculationService;
import cn.aiedge.erp.sale.service.IPriceStrategyService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 价格策略管理控制器
 */
@Tag(name = "价格策略管理")
@RestController
@RequestMapping("/api/sale/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final IPriceStrategyService priceStrategyService;
    private final IPriceCalculationService priceCalculationService;

    @Operation(summary = "创建价格策略")
    @PostMapping("/strategies")
    @SaCheckPermission("sale:pricing:create")
    public Result<Long> createStrategy(@RequestBody PriceStrategyDTO dto) {
        Long id = priceStrategyService.createStrategy(dto);
        return Result.ok("创建成功", id);
    }

    @Operation(summary = "更新价格策略")
    @PutMapping("/strategies/{id}")
    @SaCheckPermission("sale:pricing:update")
    public Result<Void> updateStrategy(@PathVariable Long id, @RequestBody PriceStrategyDTO dto) {
        priceStrategyService.updateStrategy(id, dto);
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "删除价格策略")
    @DeleteMapping("/strategies/{id}")
    @SaCheckPermission("sale:pricing:delete")
    public Result<Void> deleteStrategy(@PathVariable Long id) {
        priceStrategyService.deleteStrategy(id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "获取策略详情")
    @GetMapping("/strategies/{id}")
    @SaCheckLogin
    public Result<PriceStrategyDTO> getStrategyDetail(@PathVariable Long id) {
        PriceStrategyDTO dto = priceStrategyService.getStrategyDetail(id);
        return Result.ok(dto);
    }

    @Operation(summary = "分页查询策略列表")
    @GetMapping("/strategies")
    @SaCheckLogin
    public Result<Page<PriceStrategyDTO>> pageStrategies(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String strategyType) {
        Page<PriceStrategyDTO> page = new Page<>(pageNum, pageSize);
        Page<PriceStrategyDTO> result = priceStrategyService.pageStrategies(page, tenantId, name, status, strategyType);
        return Result.ok(result);
    }

    @Operation(summary = "查询生效中的策略")
    @GetMapping("/strategies/active")
    @SaCheckLogin
    public Result<List<PriceStrategyDTO>> getActiveStrategies() {
        List<PriceStrategyDTO> list = priceStrategyService.getActiveStrategies();
        return Result.ok(list);
    }

    @Operation(summary = "启用策略")
    @PostMapping("/strategies/{id}/activate")
    @SaCheckPermission("sale:pricing:activate")
    public Result<Void> activateStrategy(@PathVariable Long id) {
        priceStrategyService.activateStrategy(id);
        return Result.ok("启用成功", null);
    }

    @Operation(summary = "停用策略")
    @PostMapping("/strategies/{id}/deactivate")
    @SaCheckPermission("sale:pricing:activate")
    public Result<Void> deactivateStrategy(@PathVariable Long id) {
        priceStrategyService.deactivateStrategy(id);
        return Result.ok("停用成功", null);
    }

    @Operation(summary = "复制策略")
    @PostMapping("/strategies/{id}/copy")
    @SaCheckPermission("sale:pricing:create")
    public Result<Long> copyStrategy(@PathVariable Long id) {
        Long newId = priceStrategyService.copyStrategy(id);
        return Result.ok("复制成功", newId);
    }

    @Operation(summary = "计算价格")
    @PostMapping("/calculate")
    @SaCheckLogin
    public Result<PriceCalculationResult> calculatePrice(@RequestBody PriceCalculationRequest request) {
        PriceCalculationResult result = priceCalculationService.calculatePrice(request);
        return Result.ok(result);
    }

    @Operation(summary = "模拟价格计算")
    @PostMapping("/simulate")
    @SaCheckLogin
    public Result<PriceCalculationResult> simulatePrice(@RequestBody PriceCalculationRequest request) {
        PriceCalculationResult result = priceCalculationService.simulatePrice(request);
        return Result.ok(result);
    }

    @Operation(summary = "验证策略配置")
    @PostMapping("/strategies/{id}/validate")
    @SaCheckPermission("sale:pricing:validate")
    public Result<Boolean> validateStrategy(@PathVariable Long id) {
        boolean valid = priceCalculationService.validateStrategy(id);
        return Result.ok(valid);
    }

    /**
     * 统一响应结果
     */
    public static class Result<T> {
        private int code;
        private String message;
        private T data;

        public static <T> Result<T> ok(T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = "success";
            result.data = data;
            return result;
        }

        public static <T> Result<T> ok(String message, T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = message;
            result.data = data;
            return result;
        }
    }
}
