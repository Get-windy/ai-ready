package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.dto.PageResult;
import cn.aiedge.erp.b2b.dto.ProductDetailDTO;
import cn.aiedge.erp.b2b.dto.ProductListDTO;
import cn.aiedge.erp.b2b.service.MallProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mall/products")
@Tag(name = "商城商品", description = "商品浏览、搜索、分类等接口")
@RequiredArgsConstructor
public class MallProductController {

    private final MallProductService mallProductService;

    @Operation(summary = "商品列表", description = "分页获取商品列表，支持分类筛选和搜索关键词")
    @GetMapping
    public ApiResponse<PageResult<ProductListDTO>> listProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "分类ID") @RequestParam(required = false) String categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        PageResult<ProductListDTO> result = mallProductService.listProducts(page, size, categoryId, keyword);
        return ApiResponse.success(result);
    }

    @Operation(summary = "商品详情", description = "获取商品详细信息")
    @GetMapping("/{id}")
    public ApiResponse<ProductDetailDTO> getProductDetail(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        ProductDetailDTO detail = mallProductService.getProductDetail(id);
        return ApiResponse.success(detail);
    }

    @Operation(summary = "商品分类", description = "获取所有商品分类列表")
    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> getCategories() {
        List<Map<String, Object>> categories = mallProductService.getCategories();
        return ApiResponse.success(categories);
    }

    @Operation(summary = "分类商品", description = "获取指定分类下的商品列表")
    @GetMapping("/categories/{categoryId}/products")
    public ApiResponse<PageResult<ProductListDTO>> getCategoryProducts(
            @Parameter(description = "分类ID") @PathVariable String categoryId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        PageResult<ProductListDTO> result = mallProductService.listProducts(page, size, categoryId, null);
        return ApiResponse.success(result);
    }

    @Operation(summary = "搜索商品", description = "根据关键词搜索商品")
    @GetMapping("/search")
    public ApiResponse<PageResult<ProductListDTO>> searchProducts(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        PageResult<ProductListDTO> result = mallProductService.listProducts(page, size, null, keyword);
        return ApiResponse.success(result);
    }

    @Operation(summary = "推荐商品", description = "获取推荐商品列表")
    @GetMapping("/recommendations")
    public ApiResponse<List<ProductListDTO>> getRecommendations() {
        List<ProductListDTO> recommendations = mallProductService.getRecommendations();
        return ApiResponse.success(recommendations);
    }

    @Operation(summary = "热销商品", description = "获取热销商品列表")
    @GetMapping("/hot")
    public ApiResponse<List<ProductListDTO>> getHotProducts() {
        List<ProductListDTO> hotProducts = mallProductService.getHotProducts();
        return ApiResponse.success(hotProducts);
    }

    @Operation(summary = "轮播图", description = "获取首页轮播图列表")
    @GetMapping("/banners")
    public ApiResponse<List<Map<String, Object>>> getBanners() {
        List<Map<String, Object>> banners = mallProductService.getBanners();
        return ApiResponse.success(banners);
    }
}
