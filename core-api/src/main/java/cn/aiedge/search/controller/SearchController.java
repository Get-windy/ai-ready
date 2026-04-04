package cn.aiedge.search.controller;

import cn.aiedge.search.model.*;
import cn.aiedge.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索服务控制器
 * 
 * 提供全文搜索、搜索建议、搜索历史等功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索服务", description = "提供全文搜索、搜索建议、搜索历史等功能")
public class SearchController {

    private final SearchService searchService;

    /**
     * 全局搜索
     */
    @PostMapping
    @Operation(summary = "全局搜索", description = "支持客户、产品、订单等多类型搜索")
    public ResponseEntity<SearchResponse> search(
            @Valid @RequestBody SearchRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        // 填充租户和用户信息
        request.setTenantId(tenantId);
        request.setUserId(userId);
        
        log.info("全局搜索: keyword={}, type={}", request.getKeyword(), request.getType());
        
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 快速搜索（GET方式）
     */
    @GetMapping
    @Operation(summary = "快速搜索", description = "通过URL参数进行快速搜索")
    public ResponseEntity<SearchResponse> quickSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "搜索类型") @RequestParam(defaultValue = "all") String type,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        SearchRequest request = new SearchRequest();
        request.setKeyword(keyword);
        request.setType(type);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setTenantId(tenantId);
        request.setUserId(userId);
        
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索客户
     */
    @GetMapping("/customer")
    @Operation(summary = "搜索客户", description = "搜索客户信息")
    public ResponseEntity<List<SearchResult>> searchCustomer(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("搜索客户: keyword={}", keyword);
        List<SearchResult> results = searchService.searchCustomer(keyword, page, pageSize, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * 搜索产品
     */
    @GetMapping("/product")
    @Operation(summary = "搜索产品", description = "搜索产品信息")
    public ResponseEntity<List<SearchResult>> searchProduct(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("搜索产品: keyword={}", keyword);
        List<SearchResult> results = searchService.searchProduct(keyword, page, pageSize, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * 搜索订单
     */
    @GetMapping("/order")
    @Operation(summary = "搜索订单", description = "搜索订单信息")
    public ResponseEntity<List<SearchResult>> searchOrder(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("搜索订单: keyword={}", keyword);
        List<SearchResult> results = searchService.searchOrder(keyword, page, pageSize, tenantId);
        return ResponseEntity.ok(results);
    }

    /**
     * 获取搜索建议
     */
    @GetMapping("/suggestions")
    @Operation(summary = "获取搜索建议", description = "根据输入前缀获取搜索建议")
    public ResponseEntity<List<SearchSuggestion>> getSuggestions(
            @Parameter(description = "搜索前缀") @RequestParam String prefix,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        List<SearchSuggestion> suggestions = searchService.getSuggestions(prefix, limit, userId, tenantId);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * 获取热门搜索词
     */
    @GetMapping("/hot")
    @Operation(summary = "获取热门搜索词", description = "获取当前热门搜索词列表")
    public ResponseEntity<Map<String, Object>> getHotSearches(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        List<String> hotSearches = searchService.getHotSearches(limit, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("hotSearches", hotSearches);
        result.put("count", hotSearches.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    @Operation(summary = "获取搜索历史", description = "获取用户搜索历史记录")
    public ResponseEntity<List<SearchHistory>> getSearchHistory(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "20") int limit,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        List<SearchHistory> history = searchService.getSearchHistory(userId, limit, tenantId);
        return ResponseEntity.ok(history);
    }

    /**
     * 清空搜索历史
     */
    @DeleteMapping("/history")
    @Operation(summary = "清空搜索历史", description = "清空用户所有搜索历史记录")
    public ResponseEntity<Map<String, Object>> clearSearchHistory(
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        boolean success = searchService.clearSearchHistory(userId, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "搜索历史已清空" : "清空失败");
        return ResponseEntity.ok(result);
    }

    /**
     * 删除单条搜索历史
     */
    @DeleteMapping("/history/{historyId}")
    @Operation(summary = "删除单条搜索历史", description = "删除指定的搜索历史记录")
    public ResponseEntity<Map<String, Object>> deleteSearchHistory(
            @Parameter(description = "历史记录ID") @PathVariable Long historyId,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        boolean success = searchService.deleteSearchHistory(historyId, userId, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return ResponseEntity.ok(result);
    }

    /**
     * 索引客户数据
     */
    @PostMapping("/index/customer/{customerId}")
    @Operation(summary = "索引客户数据", description = "将客户数据添加到搜索索引")
    public ResponseEntity<Map<String, Object>> indexCustomer(
            @Parameter(description = "客户ID") @PathVariable Long customerId,
            @RequestBody Map<String, Object> customerData,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        searchService.indexCustomer(customerId, customerData, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "客户索引创建成功");
        result.put("customerId", customerId);
        return ResponseEntity.ok(result);
    }

    /**
     * 索引产品数据
     */
    @PostMapping("/index/product/{productId}")
    @Operation(summary = "索引产品数据", description = "将产品数据添加到搜索索引")
    public ResponseEntity<Map<String, Object>> indexProduct(
            @Parameter(description = "产品ID") @PathVariable Long productId,
            @RequestBody Map<String, Object> productData,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        searchService.indexProduct(productId, productData, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "产品索引创建成功");
        result.put("productId", productId);
        return ResponseEntity.ok(result);
    }

    /**
     * 索引订单数据
     */
    @PostMapping("/index/order/{orderId}")
    @Operation(summary = "索引订单数据", description = "将订单数据添加到搜索索引")
    public ResponseEntity<Map<String, Object>> indexOrder(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @RequestBody Map<String, Object> orderData,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        searchService.indexOrder(orderId, orderData, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "订单索引创建成功");
        result.put("orderId", orderId);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除索引
     */
    @DeleteMapping("/index/{type}/{id}")
    @Operation(summary = "删除索引", description = "从搜索索引中删除数据")
    public ResponseEntity<Map<String, Object>> deleteIndex(
            @Parameter(description = "索引类型: customer/product/order") @PathVariable String type,
            @Parameter(description = "数据ID") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        switch (type.toLowerCase()) {
            case "customer":
                searchService.deleteCustomerIndex(id, tenantId);
                break;
            case "product":
                searchService.deleteProductIndex(id, tenantId);
                break;
            case "order":
                searchService.deleteOrderIndex(id, tenantId);
                break;
            default:
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "不支持的索引类型: " + type);
                return ResponseEntity.badRequest().body(errorResult);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "索引删除成功");
        return ResponseEntity.ok(result);
    }

    /**
     * 重建索引
     */
    @PostMapping("/index/rebuild/{type}")
    @Operation(summary = "重建索引", description = "重建指定类型的搜索索引")
    public ResponseEntity<Map<String, Object>> rebuildIndex(
            @Parameter(description = "索引类型: customer/product/order/all") @PathVariable String type,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        long count;
        if ("all".equalsIgnoreCase(type)) {
            count = searchService.rebuildIndex("customer", tenantId);
            count += searchService.rebuildIndex("product", tenantId);
            count += searchService.rebuildIndex("order", tenantId);
        } else {
            count = searchService.rebuildIndex(type, tenantId);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "索引重建完成");
        result.put("count", count);
        return ResponseEntity.ok(result);
    }
}
