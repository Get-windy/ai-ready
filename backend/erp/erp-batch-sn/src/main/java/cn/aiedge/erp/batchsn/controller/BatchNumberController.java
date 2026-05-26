package cn.aiedge.erp.batchsn.controller;

import cn.aiedge.erp.batchsn.controller.dto.BatchQueryRequest;
import cn.aiedge.erp.batchsn.controller.dto.CreateBatchRequest;
import cn.aiedge.erp.batchsn.controller.dto.BatchTransferRequest;
import cn.aiedge.erp.batchsn.controller.dto.BatchInventoryRequest;
import cn.aiedge.erp.batchsn.controller.dto.BatchAdvancedSearchRequest;
import cn.aiedge.erp.batchsn.controller.response.ApiResponse;
import cn.aiedge.erp.batchsn.controller.util.ApiCacheManager;
import cn.aiedge.erp.batchsn.entity.BatchNumber;
import cn.aiedge.erp.batchsn.service.BatchNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 批次号管理控制器（优化版）
 * 
 * @author team-member
 * @date 2026-04-27
 * @updated 2026-05-05 - 添加输入验证、缓存支持、统一响应格式
 */
@RestController
@RequestMapping("/api/erp/batch-sn/batches")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "批次管理", description = "批次CRUD操作、入库出库、转移盘点等接口")
public class BatchNumberController {
    
    private final BatchNumberService batchNumberService;
    private final ApiCacheManager cacheManager;
    
    /**
     * 创建批次
     */
    @Operation(
        summary = "创建批次",
        description = "创建新的批次记录，支持自动生成批次号"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "批次创建成功", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BatchNumber>> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        log.info("创建批次请求: {}", request);
        
        // 转换请求对象为实体
        BatchNumber batch = convertToEntity(request);
        BatchNumber created = batchNumberService.createBatch(batch);
        
        // 清除相关缓存
        cacheManager.clearAllListCache();
        
        log.info("批次创建成功: ID={}, BatchNo={}", created.getId(), created.getBatchNo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created));
    }
    
    /**
     * 查询批次详情（带缓存）
     */
    @Operation(
        summary = "查询批次详情",
        description = "根据批次ID查询批次详情信息，支持缓存"
    )
    @Parameter(name = "id", description = "批次ID", required = true, example = "1")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "批次详情查询成功"),
        @ApiResponse(responseCode = "404", description = "批次未找到"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BatchNumber>> getBatch(@PathVariable @NotNull Long id) {
        log.debug("查询批次详情: ID={}", id);
        
        // 尝试从缓存获取
        BatchNumber cachedBatch = cacheManager.getBatchCache(id);
        if (cachedBatch != null) {
            log.debug("从缓存获取批次: ID={}", id);
            return ResponseEntity.ok(ApiResponse.success(cachedBatch));
        }
        
        BatchNumber batch = batchNumberService.getBatchById(id);
        if (batch == null) {
            log.warn("批次未找到: ID={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("批次未找到"));
        }
        
        // 设置缓存
        cacheManager.setBatchCache(id, batch);
        
        return ResponseEntity.ok(ApiResponse.success(batch));
    }
    
    /**
     * 查询批次列表（带缓存和分页）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BatchNumber>>> listBatches(
        @Valid BatchQueryRequest request
    ) {
        log.debug("查询批次列表: {}", request);
        
        // 验证和规范化请求参数
        request.validate();
        
        // 生成查询哈希用于缓存
        String queryHash = cacheManager.generateQueryHash(request);
        
        // 尝试从缓存获取
        List<BatchNumber> cachedBatches = cacheManager.getListCache(queryHash);
        if (cachedBatches != null) {
            log.debug("从缓存获取批次列表: queryHash={}, count={}", queryHash, cachedBatches.size());
            return ResponseEntity.ok(ApiResponse.success(cachedBatches));
        }
        
        // 查询数据
        List<BatchNumber> batches = batchNumberService.listBatches(
            request.getBatchNo(),
            request.getProductCode(),
            request.getStatus(),
            request.getSourceType(),
            request.getPage(),
            request.getSize()
        );
        
        // 设置缓存
        cacheManager.setListCache(queryHash, batches);
        
        log.debug("批次列表查询成功: count={}", batches.size());
        return ResponseEntity.ok(ApiResponse.success(batches));
    }
    
    /**
     * 更新批次信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BatchNumber>> updateBatch(
        @PathVariable @NotNull Long id,
        @Valid @RequestBody CreateBatchRequest request
    ) {
        log.info("更新批次信息: ID={}, request={}", id, request);
        
        // 转换请求对象为实体
        BatchNumber batch = convertToEntity(request);
        batch.setId(id);
        
        BatchNumber updated = batchNumberService.updateBatch(id, batch);
        if (updated == null) {
            log.warn("批次更新失败: ID={} 未找到", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("批次未找到"));
        }
        
        // 清除相关缓存
        cacheManager.evictBatchCache(id);
        cacheManager.clearAllListCache();
        
        log.info("批次更新成功: ID={}", id);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
    
    /**
     * 批量更新批次状态
     */
    @PatchMapping("/status")
    public ResponseEntity<ApiResponse<Integer>> updateBatchStatus(
        @NotNull @RequestBody List<Long> batchIds,
        @NotBlank @RequestParam String newStatus
    ) {
        log.info("批量更新批次状态: batchIds={}, newStatus={}", batchIds, newStatus);
        
        if (batchIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("批次ID列表不能为空"));
        }
        
        int updated = batchNumberService.updateBatchStatus(batchIds, newStatus);
        
        // 清除相关缓存
        batchIds.forEach(cacheManager::evictBatchCache);
        cacheManager.clearAllListCache();
        
        log.info("批次状态批量更新完成: updatedCount={}", updated);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
    
    /**
     * 入库操作
     */
    @Operation(
        summary = "批次入库",
        description = "执行批次入库操作，支持新批次创建和现有批次数量增加"
    )
    @Parameter(name = "warehouseId", description = "仓库ID", required = true, example = "1")
    @Parameter(name = "warehouseName", description = "仓库名称", required = true, example = "主仓库")
    @Parameter(name = "locationId", description = "库位ID", example = "10")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "入库成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/inbound")
    public ResponseEntity<ApiResponse<BatchNumber>> inbound(
        @Valid @RequestBody CreateBatchRequest request,
        @NotNull @RequestParam Long warehouseId,
        @NotBlank @RequestParam String warehouseName,
        @RequestParam(required = false) Long locationId
    ) {
        log.info("批次入库请求: batchNo={}, warehouseId={}", request.getBatchNo(), warehouseId);
        
        BatchNumber batch = convertToEntity(request);
        BatchNumber result = batchNumberService.inbound(batch, warehouseId, warehouseName, locationId);
        
        // 清除相关缓存
        cacheManager.clearAllListCache();
        
        log.info("批次入库成功: ID={}, BatchNo={}", result.getId(), result.getBatchNo());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 出库操作
     */
    @Operation(
        summary = "批次出库",
        description = "执行批次出库操作，验证库存数量，扣减可用库存"
    )
    @Parameter(name = "batchId", description = "批次ID", required = true, example = "1")
    @Parameter(name = "quantity", description = "出库数量", required = true, example = "10.5")
    @Parameter(name = "warehouseId", description = "仓库ID", required = true, example = "1")
    @Parameter(name = "locationId", description = "库位ID", example = "10")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "出库成功"),
        @ApiResponse(responseCode = "400", description = "出库数量必须大于0"),
        @ApiResponse(responseCode = "404", description = "批次未找到或库存不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/outbound")
    public ResponseEntity<ApiResponse<BatchNumber>> outbound(
        @NotNull @RequestParam Long batchId,
        @NotNull @RequestParam BigDecimal quantity,
        @NotNull @RequestParam Long warehouseId,
        @RequestParam(required = false) Long locationId
    ) {
        log.info("批次出库请求: batchId={}, quantity={}", batchId, quantity);
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("出库数量必须大于0"));
        }
        
        BatchNumber result = batchNumberService.outbound(batchId, quantity, warehouseId, locationId);
        if (result == null) {
            log.warn("批次出库失败: batchId={} 未找到或库存不足", batchId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("批次未找到或库存不足"));
        }
        
        // 清除相关缓存
        cacheManager.evictBatchCache(batchId);
        cacheManager.clearAllListCache();
        
        log.info("批次出库成功: batchId={}, quantity={}", batchId, quantity);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 质检操作
     */
    @PostMapping("/{id}/quality-inspection")
    public ResponseEntity<ApiResponse<BatchNumber>> qualityInspection(
        @PathVariable @NotNull Long id,
        @NotBlank @RequestParam String status,
        @NotBlank @RequestParam String inspectorId,
        @NotBlank @RequestParam String inspectorName
    ) {
        log.info("批次质检请求: batchId={}, status={}, inspector={}", id, status, inspectorName);
        
        BatchNumber result = batchNumberService.qualityInspection(id, status, inspectorId, inspectorName);
        if (result == null) {
            log.warn("批次质检失败: batchId={} 未找到", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("批次未找到"));
        }
        
        // 清除相关缓存
        cacheManager.evictBatchCache(id);
        cacheManager.clearAllListCache();
        
        log.info("批次质检成功: batchId={}, status={}", id, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 查询即将过期的批次（临期预警）
     */
    @GetMapping("/expiring-warning")
    public ResponseEntity<ApiResponse<List<BatchNumber>>> getExpiringBatches(
        @RequestParam(defaultValue = "30") @Min(1) int warningDays
    ) {
        log.debug("查询临期批次: warningDays={}", warningDays);
        
        List<BatchNumber> batches = batchNumberService.getExpiringBatches(warningDays);
        
        log.debug("临期批次查询成功: count={}", batches.size());
        return ResponseEntity.ok(ApiResponse.success(batches));
    }
    
    /**
     * 查询批次库存汇总
     */
    @GetMapping("/stock-summary")
    public ResponseEntity<ApiResponse<List<BatchNumber>>> getStockSummary() {
        log.debug("查询批次库存汇总");
        
        List<BatchNumber> summary = batchNumberService.getStockSummary();
        
        log.debug("批次库存汇总查询成功: count={}", summary.size());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * 验证批次号唯一性
     */
    @GetMapping("/validate-batch-no")
    public ResponseEntity<ApiResponse<Boolean>> validateBatchNo(
        @NotBlank @RequestParam String batchNo
    ) {
        log.debug("验证批次号唯一性: batchNo={}", batchNo);
        
        boolean exists = batchNumberService.batchNoExists(batchNo);
        
        log.debug("批次号验证结果: batchNo={}, exists={}", batchNo, exists);
        return ResponseEntity.ok(ApiResponse.success(!exists));
    }
    
    /**
     * 获取缓存统计信息（仅用于调试）
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<ApiResponse<ApiCacheManager.CacheStats>> getCacheStats() {
        ApiCacheManager.CacheStats stats = cacheManager.getCacheStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 清除所有缓存（仅用于调试）
     */
    @DeleteMapping("/clear-cache")
    public ResponseEntity<ApiResponse<String>> clearCache() {
        cacheManager.clearAllCache();
        log.info("批次缓存已全部清除");
        return ResponseEntity.ok(ApiResponse.success("缓存已清除"));
    }
    
    /**
     * 批次转移操作
     */
    @Operation(
        summary = "批次转移",
        description = "执行批次在不同仓库/库位间的转移操作"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "转移成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "404", description = "批次未找到或库存不足"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<BatchNumber>> transferBatch(
        @Valid @RequestBody BatchTransferRequest request
    ) {
        log.info("批次转移请求: {}", request);
        
        // 验证请求参数
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
        
        BatchNumber result = batchNumberService.transfer(
            request.getBatchId(),
            request.getFromWarehouseId(),
            request.getToWarehouseId(),
            request.getFromLocationId(),
            request.getToLocationId(),
            request.getQuantity(),
            request.getRemark()
        );
        
        if (result == null) {
            log.warn("批次转移失败: batchId={} 未找到或库存不足", request.getBatchId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("批次未找到或库存不足"));
        }
        
        // 清除相关缓存
        cacheManager.evictBatchCache(request.getBatchId());
        cacheManager.clearAllListCache();
        
        log.info("批次转移成功: batchId={}, fromWarehouse={}, toWarehouse={}", 
            request.getBatchId(), request.getFromWarehouseId(), request.getToWarehouseId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 批次盘点操作
     */
    @Operation(
        summary = "批次盘点",
        description = "执行批次盘点操作，记录实际盘点数量和系统数量的差异"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "盘点成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "404", description = "批次未找到"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse<BatchNumber>> inventoryBatch(
        @Valid @RequestBody BatchInventoryRequest request
    ) {
        log.info("批次盘点请求: {}", request);
        
        // 验证请求参数
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
        
        BatchNumber result = batchNumberService.inventory(
            request.getBatchId(),
            request.getPhysicalQuantity(),
            request.getAdjustmentQuantity(),
            request.getAdjustmentReason(),
            request.getOperatorId(),
            request.getOperatorName()
        );
        
        if (result == null) {
            log.warn("批次盘点失败: batchId={} 未找到", request.getBatchId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("批次未找到"));
        }
        
        // 清除相关缓存
        cacheManager.evictBatchCache(request.getBatchId());
        cacheManager.clearAllListCache();
        
        log.info("批次盘点完成: batchId={}, physicalQty={}", 
            request.getBatchId(), request.getPhysicalQuantity());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * 高级搜索（支持复杂查询条件）
     */
    @Operation(
        summary = "批次高级搜索",
        description = "支持多条件复杂查询，包括日期范围、状态、仓库等筛选条件"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<BatchNumber>>> advancedSearch(
        @Valid @RequestBody BatchAdvancedSearchRequest request
    ) {
        log.debug("批次高级搜索请求: {}", request);
        
        // 验证请求参数
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
        
        // 检查是否是导出请求
        if (request.isExport()) {
            // 导出逻辑（这里只返回数据，实际导出需要专门处理）
            request.setSize(Integer.MAX_VALUE); // 导出时获取所有数据
            log.info("批次导出请求: query={}", request);
        }
        
        List<BatchNumber> batches = batchNumberService.searchBatches(
            request.getBatchNo(),
            request.getProductCode(),
            request.getProductName(),
            request.getStatus(),
            request.getQualityStatus(),
            request.getSourceType(),
            request.getProductionDateStart(),
            request.getProductionDateEnd(),
            request.getExpirationDateStart(),
            request.getExpirationDateEnd(),
            request.getWarehouseId(),
            request.getWarehouseName(),
            request.getSortField(),
            request.getSortDirection(),
            request.getPage(),
            request.getSize()
        );
        
        log.debug("批次高级搜索完成: count={}", batches.size());
        return ResponseEntity.ok(ApiResponse.success(batches));
    }
    
    /**
     * 批次数据导出
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportBatches(
        @Valid BatchQueryRequest request
    ) {
        log.info("批次数据导出请求: {}", request);
        
        // 暂时返回JSON格式，实际应该返回CSV或Excel文件
        // TODO: 实现CSV或Excel导出
        List<BatchNumber> batches = batchNumberService.listBatches(
            request.getBatchNo(),
            request.getProductCode(),
            request.getStatus(),
            request.getSourceType(),
            1, // 导出时使用第一页
            Integer.MAX_VALUE // 导出时获取所有数据
        );
        
        log.info("批次导出完成: count={}", batches.size());
        return ResponseEntity.ok(ApiResponse.success(batches));
    }
    
    /**
     * 将请求DTO转换为实体
     */
    private BatchNumber convertToEntity(CreateBatchRequest request) {
        BatchNumber batch = new BatchNumber();
        batch.setBatchNo(request.getBatchNo());
        batch.setProductId(request.getProductId());
        batch.setProductCode(request.getProductCode());
        batch.setProductName(request.getProductName());
        batch.setSpecification(request.getSpecification());
        batch.setUnit(request.getUnit());
        batch.setProductionDate(request.getProductionDate());
        batch.setExpirationDate(request.getExpirationDate());
        batch.setBatchStatus(request.getBatchStatus());
        batch.setTotalQuantity(request.getTotalQuantity());
        batch.setAvailableQuantity(request.getAvailableQuantity());
        batch.setReservedQuantity(request.getReservedQuantity());
        batch.setSourceType(request.getSourceType());
        batch.setSourceRefNo(request.getSourceRefNo());
        batch.setWarehouseId(request.getWarehouseId());
        batch.setWarehouseName(request.getWarehouseName());
        batch.setLocationId(request.getLocationId());
        batch.setQualityStatus(request.getQualityStatus());
        return batch;
    }
}
