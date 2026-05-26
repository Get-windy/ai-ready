package cn.aiedge.erp.sale.controller;

import cn.aiedge.erp.sale.dto.PromotionActivityDTO;
import cn.aiedge.erp.sale.service.IPromotionService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 促销活动管理控制器
 */
@Tag(name = "促销活动管理")
@RestController
@RequestMapping("/api/sale/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final IPromotionService promotionService;

    @Operation(summary = "创建促销活动")
    @PostMapping
    @SaCheckPermission("sale:promotion:create")
    public Result<Long> createPromotion(@RequestBody PromotionActivityDTO dto) {
        Long id = promotionService.createPromotion(dto);
        return Result.ok("创建成功", id);
    }

    @Operation(summary = "更新促销活动")
    @PutMapping("/{id}")
    @SaCheckPermission("sale:promotion:update")
    public Result<Void> updatePromotion(@PathVariable Long id, @RequestBody PromotionActivityDTO dto) {
        promotionService.updatePromotion(id, dto);
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "删除促销活动")
    @DeleteMapping("/{id}")
    @SaCheckPermission("sale:promotion:delete")
    public Result<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "获取活动详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public Result<PromotionActivityDTO> getPromotionDetail(@PathVariable Long id) {
        PromotionActivityDTO dto = promotionService.getPromotionDetail(id);
        return Result.ok(dto);
    }

    @Operation(summary = "分页查询活动列表")
    @GetMapping("/page")
    @SaCheckLogin
    public Result<Page<PromotionActivityDTO>> pagePromotions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        Page<PromotionActivityDTO> page = new Page<>(pageNum, pageSize);
        Page<PromotionActivityDTO> result = promotionService.pagePromotions(page, tenantId, name, status, type);
        return Result.ok(result);
    }

    @Operation(summary = "发布促销")
    @PostMapping("/{id}/publish")
    @SaCheckPermission("sale:promotion:publish")
    public Result<Void> publishPromotion(@PathVariable Long id) {
        promotionService.publishPromotion(id);
        return Result.ok("发布成功", null);
    }

    @Operation(summary = "取消促销")
    @PostMapping("/{id}/cancel")
    @SaCheckPermission("sale:promotion:publish")
    public Result<Void> cancelPromotion(@PathVariable Long id) {
        promotionService.cancelPromotion(id);
        return Result.ok("取消成功", null);
    }

    @Operation(summary = "获取当前生效的促销")
    @GetMapping("/active")
    @SaCheckLogin
    public Result<List<PromotionActivityDTO>> getActivePromotions(@RequestParam Long tenantId) {
        List<PromotionActivityDTO> list = promotionService.getActivePromotions(tenantId);
        return Result.ok(list);
    }

    @Operation(summary = "获取适用的促销")
    @GetMapping("/applicable")
    @SaCheckLogin
    public Result<List<PromotionActivityDTO>> getApplicablePromotions(
            @RequestParam Long tenantId,
            @RequestParam Long productId,
            @RequestParam(required = false) String customerLevel) {
        List<PromotionActivityDTO> list = promotionService.getApplicablePromotions(tenantId, productId, customerLevel);
        return Result.ok(list);
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
