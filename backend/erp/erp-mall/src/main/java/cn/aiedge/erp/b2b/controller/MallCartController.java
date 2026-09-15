package cn.aiedge.erp.b2b.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.dto.CartAddRequest;
import cn.aiedge.erp.b2b.dto.CartDTO;
import cn.aiedge.erp.b2b.service.MallCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mall/cart")
@Tag(name = "商城购物车", description = "购物车管理接口")
@RequiredArgsConstructor
public class MallCartController {

    private final MallCartService mallCartService;

    @Operation(summary = "获取购物车", description = "获取当前用户的购物车列表")
    @GetMapping
    public ApiResponse<List<CartDTO>> getCart() {
        List<CartDTO> cartList = mallCartService.getCart();
        return ApiResponse.success(cartList);
    }

    @Operation(summary = "购物车分页查询（管理端）",
            description = "真实分页 SQL；支持会员（登录名/昵称/手机号/公司名）与商品（名称/编码）关键字过滤，"
                    + "额外返回会员昵称 memberName 与账号 memberAccount（由 shop_user 派生，未新增列）")
    @GetMapping("/page")
    public ApiResponse<PageResult<CartDTO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "会员ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "会员关键字（登录名/昵称/手机号/公司名）") @RequestParam(required = false) String memberKeyword,
            @Parameter(description = "商品关键字（名称/编码）") @RequestParam(required = false) String productKeyword) {
        return ApiResponse.success(mallCartService.pageCart(pageNum, pageSize, customerId, memberKeyword, productKeyword));
    }

    @Operation(summary = "添加购物车", description = "将商品添加到购物车")
    @PostMapping
    public ApiResponse<CartDTO> addToCart(@RequestBody CartAddRequest request) {
        CartDTO cartItem = mallCartService.addToCart(request);
        return ApiResponse.success("添加成功", cartItem);
    }

    @Operation(summary = "更新购物车", description = "更新购物车中商品的数量等信息")
    @PutMapping("/{id}")
    public ApiResponse<CartDTO> updateCartItem(
            @Parameter(description = "购物车项ID") @PathVariable Long id,
            @RequestBody CartAddRequest request) {
        CartDTO updated = mallCartService.updateCartItem(id, request);
        return ApiResponse.success("更新成功", updated);
    }

    @Operation(summary = "删除购物车项", description = "删除购物车中的指定商品")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeFromCart(
            @Parameter(description = "购物车项ID") @PathVariable Long id) {
        mallCartService.removeFromCart(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "清空购物车", description = "清空当前用户的整个购物车")
    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        mallCartService.clearCart();
        return ApiResponse.success("购物车已清空", null);
    }

    @Operation(summary = "批量删除购物车项", description = "请求体为购物车项ID数组，如 [1,2,3]；逻辑删除，返回实际删除条数")
    @DeleteMapping("/batch")
    public ApiResponse<Integer> removeBatch(@RequestBody List<Long> ids) {
        int removed = mallCartService.removeBatch(ids);
        return ApiResponse.success("已删除 " + removed + " 项", removed);
    }

    @Operation(summary = "检查库存", description = "检查购物车中商品的库存是否充足")
    @PostMapping("/check-stock")
    public ApiResponse<Void> checkStock() {
        mallCartService.checkStock();
        return ApiResponse.success("库存充足", null);
    }
}
