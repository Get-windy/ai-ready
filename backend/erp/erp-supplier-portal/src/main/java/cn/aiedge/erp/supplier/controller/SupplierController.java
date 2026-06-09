package cn.aiedge.erp.supplier.controller;

import cn.aiedge.erp.supplier.dto.SupplierDTO;
import cn.aiedge.erp.supplier.dto.SupplierQueryDTO;
import cn.aiedge.erp.supplier.dto.SupplierPerformanceDTO;
import cn.aiedge.erp.supplier.service.SupplierService;
import cn.aiedge.common.core.domain.PageResult;
import cn.aiedge.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 供应商管理控制器
 * 提供供应商相关的RESTful API接口
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/supplier")
@Tag(name = "供应商管理", description = "供应商信息的增删改查、绩效评估、门户管理等接口")
public class SupplierController {
    
    private final SupplierService supplierService;
    
    @PostMapping
    @Operation(summary = "创建供应商", description = "创建新的供应商信息")
    public R<SupplierDTO> createSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
        log.info("创建供应商请求：{}", supplierDTO.getSupplierCode());
        return supplierService.createSupplier(supplierDTO);
    }
    
    @PutMapping
    @Operation(summary = "更新供应商", description = "更新现有供应商信息")
    public R<SupplierDTO> updateSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
        log.info("更新供应商请求：{}", supplierDTO.getId());
        return supplierService.updateSupplier(supplierDTO);
    }
    
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获取供应商详情", description = "根据ID获取供应商详细信息")
    public R<SupplierDTO> getSupplierById(
            @Parameter(description = "供应商ID", required = true) 
            @PathVariable Long id) {
        log.info("获取供应商详情请求：{}", id);
        return supplierService.getSupplierById(id);
    }
    
    @GetMapping("/code/{supplierCode}")
    @Operation(summary = "根据编码获取供应商", description = "根据供应商编码获取详细信息")
    public R<SupplierDTO> getSupplierByCode(
            @Parameter(description = "供应商编码", required = true)
            @PathVariable String supplierCode) {
        log.info("根据编码获取供应商请求：{}", supplierCode);
        return supplierService.getSupplierByCode(supplierCode);
    }
    
    @PostMapping("/page")
    @Operation(summary = "分页查询供应商", description = "根据条件分页查询供应商列表")
    public R<PageResult<SupplierDTO>> querySupplierPage(@Valid @RequestBody SupplierQueryDTO queryDTO) {
        log.info("分页查询供应商请求：页码={}, 大小={}", queryDTO.getPageNum(), queryDTO.getPageSize());
        return supplierService.querySupplierPage(queryDTO);
    }
    
    @PostMapping("/list")
    @Operation(summary = "查询供应商列表", description = "根据条件查询供应商列表（不分页，POST方式，带查询条件）")
    public R<List<SupplierDTO>> querySupplierList(@Valid @RequestBody SupplierQueryDTO queryDTO) {
        log.info("查询供应商列表请求");
        return supplierService.querySupplierList(queryDTO);
    }

    @GetMapping("/list")
    @Operation(summary = "查询供应商列表", description = "获取所有可用供应商列表（GET方式，供下拉选择器使用）")
    public R<List<SupplierDTO>> listAll() {
        log.info("查询所有供应商列表请求");
        return supplierService.querySupplierList(new SupplierQueryDTO());
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除供应商", description = "逻辑删除供应商信息")
    public R<Boolean> deleteSupplier(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id) {
        log.info("删除供应商请求：{}", id);
        return supplierService.deleteSupplier(id);
    }
    
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除供应商", description = "批量逻辑删除供应商信息")
    public R<Boolean> batchDeleteSupplier(
            @Parameter(description = "供应商ID列表", required = true)
            @RequestBody List<Long> ids) {
        log.info("批量删除供应商请求：{}条", ids.size());
        return supplierService.batchDeleteSupplier(ids);
    }
    
    @PostMapping("/{id}/activate-portal")
    @Operation(summary = "激活供应商门户", description = "激活供应商门户账户")
    public R<Boolean> activateSupplierPortal(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "门户账户ID", required = true)
            @RequestParam String portalAccountId) {
        log.info("激活供应商门户请求：{}，账户ID：{}", id, portalAccountId);
        return supplierService.activateSupplierPortal(id, portalAccountId);
    }
    
    @PostMapping("/{id}/disable-portal")
    @Operation(summary = "禁用供应商门户", description = "禁用供应商门户账户")
    public R<Boolean> disableSupplierPortal(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "禁用原因", required = true)
            @RequestParam String reason) {
        log.info("禁用供应商门户请求：{}，原因：{}", id, reason);
        return supplierService.disableSupplierPortal(id, reason);
    }
    
    @PostMapping("/{id}/update-level")
    @Operation(summary = "更新供应商等级", description = "更新供应商等级")
    public R<Boolean> updateSupplierLevel(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "供应商等级", required = true)
            @RequestParam String supplierLevel,
            @Parameter(description = "等级变更原因", required = true)
            @RequestParam String reason) {
        log.info("更新供应商等级请求：{}，新等级：{}，原因：{}", id, supplierLevel, reason);
        return supplierService.updateSupplierLevel(id, supplierLevel, reason);
    }
    
    @PostMapping("/{id}/update-cooperation-status")
    @Operation(summary = "更新合作状态", description = "更新供应商合作状态")
    public R<Boolean> updateCooperationStatus(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "合作状态", required = true)
            @RequestParam Integer cooperationStatus,
            @Parameter(description = "状态变更原因", required = true)
            @RequestParam String reason) {
        log.info("更新合作状态请求：{}，新状态：{}，原因：{}", id, cooperationStatus, reason);
        return supplierService.updateCooperationStatus(id, cooperationStatus, reason);
    }
    
    @PostMapping("/performance/evaluate")
    @Operation(summary = "评估供应商绩效", description = "对供应商进行绩效评估")
    public R<Boolean> evaluateSupplierPerformance(@Valid @RequestBody SupplierPerformanceDTO performanceDTO) {
        log.info("评估供应商绩效请求：供应商ID={}", performanceDTO.getSupplierId());
        return supplierService.evaluateSupplierPerformance(performanceDTO);
    }
    
    @GetMapping("/{supplierId}/performance/history")
    @Operation(summary = "获取绩效历史", description = "获取供应商绩效评估历史记录")
    public R<List<SupplierPerformanceDTO>> getSupplierPerformanceHistory(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long supplierId,
            @Parameter(description = "周期类型：1-月度，2-季度，3-年度")
            @RequestParam(required = false) Integer periodType,
            @Parameter(description = "限制条数")
            @RequestParam(required = false) Integer limit) {
        log.info("获取绩效历史请求：供应商ID={}", supplierId);
        return supplierService.getSupplierPerformanceHistory(supplierId, periodType, limit);
    }
    
    @GetMapping("/{supplierId}/comprehensive-score")
    @Operation(summary = "获取综合评分", description = "获取供应商综合评分")
    public R<Double> getSupplierComprehensiveScore(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long supplierId) {
        log.info("获取综合评分请求：供应商ID={}", supplierId);
        return supplierService.getSupplierComprehensiveScore(supplierId);
    }
    
    @PostMapping("/import")
    @Operation(summary = "导入供应商", description = "批量导入供应商数据")
    public R<Boolean> importSuppliers(
            @Parameter(description = "供应商数据列表", required = true)
            @RequestBody List<SupplierDTO> supplierList) {
        log.info("导入供应商请求：{}条", supplierList.size());
        return supplierService.importSuppliers(supplierList);
    }
    
    @PostMapping("/export")
    @Operation(summary = "导出供应商", description = "导出供应商数据")
    public R<List<SupplierDTO>> exportSuppliers(@Valid @RequestBody SupplierQueryDTO queryDTO) {
        log.info("导出供应商请求");
        return supplierService.exportSuppliers(queryDTO);
    }
    
    @PostMapping("/validate")
    @Operation(summary = "验证供应商", description = "验证供应商信息有效性")
    public R<Boolean> validateSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
        log.info("验证供应商请求：{}", supplierDTO.getSupplierCode());
        return supplierService.validateSupplier(supplierDTO);
    }
    
    @PostMapping("/{id}/sync-portal")
    @Operation(summary = "同步门户账户", description = "同步供应商门户账户信息")
    public R<Boolean> syncSupplierPortalAccount(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id) {
        log.info("同步门户账户请求：{}", id);
        return supplierService.syncSupplierPortalAccount(id);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "获取统计信息", description = "获取供应商统计信息")
    public R<Map<String, Object>> getSupplierStatistics() {
        log.info("获取供应商统计信息请求");
        // 从SecurityUtils获取tenantId
        String tenantId = "default"; // 实际应从SecurityUtils获取
        return supplierService.getSupplierStatistics(tenantId);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取统计信息(别名)", description = "与/statistics相同，兼容前端不同拼写")
    public R<Map<String, Object>> getSupplierStats() {
        return getSupplierStatistics();
    }
}