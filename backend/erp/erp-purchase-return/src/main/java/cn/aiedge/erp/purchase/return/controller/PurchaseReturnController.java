package cn.aiedge.erp.purchase.return.controller;

import cn.aiedge.erp.purchase.return.dto.PurchaseReturnDTO;
import cn.aiedge.erp.purchase.return.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.return.service.PurchaseReturnService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 采购换货单控制器
 * 
 * 功能: 提供采购换货单的RESTful API接口
 * API路径: /api/erp/purchase/return
 */
@RestController
@RequestMapping("/return")
@RequiredArgsConstructor
@Tag(name = "采购换货管理", description = "采购换货单的创建、审批、执行、跟踪等全流程管理")
@Validated
public class PurchaseReturnController {
    
    private final PurchaseReturnService purchaseReturnService;
    
    /**
     * 创建采购换货单
     */
    @PostMapping
    @Operation(summary = "创建采购换货单", description = "创建新的采购换货申请单")
    @PreAuthorize("hasPermission('purchase_return', 'create')")
    public ResponseEntity<Map<String, Object>> createPurchaseReturn(
            @Valid @RequestBody PurchaseReturnDTO dto) {
        Long returnId = purchaseReturnService.createPurchaseReturn(dto);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "采购换货单创建成功",
            "data", Map.of("returnId", returnId)
        ));
    }
    
    /**
     * 更新采购换货单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新采购换货单", description = "更新指定ID的采购换货单")
    @PreAuthorize("hasPermission('purchase_return', 'update')")
    public ResponseEntity<Map<String, Object>> updatePurchaseReturn(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseReturnDTO dto) {
        boolean success = purchaseReturnService.updatePurchaseReturn(id, dto);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "采购换货单更新成功" : "采购换货单更新失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 获取采购换货单详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取采购换货单详情", description = "获取指定ID的采购换货单详细信息")
    @PreAuthorize("hasPermission('purchase_return', 'read')")
    public ResponseEntity<Map<String, Object>> getPurchaseReturn(@PathVariable Long id) {
        PurchaseReturn purchaseReturn = purchaseReturnService.getById(id);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "获取成功",
            "data", purchaseReturn
        ));
    }
    
    /**
     * 删除采购换货单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除采购换货单", description = "删除指定ID的采购换货单")
    @PreAuthorize("hasPermission('purchase_return', 'delete')")
    public ResponseEntity<Map<String, Object>> deletePurchaseReturn(@PathVariable Long id) {
        boolean success = purchaseReturnService.removeById(id);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "采购换货单删除成功" : "采购换货单删除失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 分页查询采购换货单列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询采购换货单列表", description = "分页查询采购换货单列表，支持多种筛选条件")
    @PreAuthorize("hasPermission('purchase_return', 'read')")
    public ResponseEntity<Map<String, Object>> getPurchaseReturnList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String returnCode,
            @RequestParam(required = false) String purchaseOrderCode,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String returnType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Page<PurchaseReturn> pageParam = new Page<>(page, size);
        IPage<PurchaseReturn> result = purchaseReturnService.page(pageParam);
        
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "查询成功",
            "data", result
        ));
    }
    
    /**
     * 提交换货单审批
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交换货单审批", description = "将换货单提交到审批流程")
    @PreAuthorize("hasPermission('purchase_return', 'submit')")
    public ResponseEntity<Map<String, Object>> submitForApproval(@PathVariable Long id) {
        String processInstanceId = purchaseReturnService.submitForApproval(id);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "换货单提交审批成功",
            "data", Map.of("processInstanceId", processInstanceId)
        ));
    }
    
    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过", description = "审批通过换货单")
    @PreAuthorize("hasPermission('purchase_return', 'approve')")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable Long id,
            @RequestParam Long approverId,
            @RequestParam String approverName,
            @RequestParam(required = false) String comment) {
        boolean success = purchaseReturnService.approve(id, approverId, approverName, comment);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "审批通过成功" : "审批通过失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 审批拒绝
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝", description = "审批拒绝换货单")
    @PreAuthorize("hasPermission('purchase_return', 'approve')")
    public ResponseEntity<Map<String, Object>> reject(
            @PathVariable Long id,
            @RequestParam Long approverId,
            @RequestParam String approverName,
            @RequestParam String comment) {
        boolean success = purchaseReturnService.reject(id, approverId, approverName, comment);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "审批拒绝成功" : "审批拒绝失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 供应商确认换货方案
     */
    @PostMapping("/{id}/supplier/confirm")
    @Operation(summary = "供应商确认换货方案", description = "供应商确认换货处理方案")
    @PreAuthorize("hasPermission('purchase_return', 'supplier_confirm')")
    public ResponseEntity<Map<String, Object>> supplierConfirm(
            @PathVariable Long id,
            @RequestParam String confirmationNote) {
        boolean success = purchaseReturnService.supplierConfirm(id, confirmationNote);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "供应商确认成功" : "供应商确认失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 供应商拒绝换货方案
     */
    @PostMapping("/{id}/supplier/reject")
    @Operation(summary = "供应商拒绝换货方案", description = "供应商拒绝换货处理方案")
    @PreAuthorize("hasPermission('purchase_return', 'supplier_confirm')")
    public ResponseEntity<Map<String, Object>> supplierReject(
            @PathVariable Long id,
            @RequestParam String rejectionReason) {
        boolean success = purchaseReturnService.supplierReject(id, rejectionReason);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "供应商拒绝成功" : "供应商拒绝失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 更新退货物流信息
     */
    @PutMapping("/{id}/logistics/return")
    @Operation(summary = "更新退货物流信息", description = "更新退货物流单号和物流公司")
    @PreAuthorize("hasPermission('purchase_return', 'update')")
    public ResponseEntity<Map<String, Object>> updateReturnLogistics(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam String logisticsCompany) {
        boolean success = purchaseReturnService.updateReturnLogistics(id, trackingNumber, logisticsCompany);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "退货物流信息更新成功" : "退货物流信息更新失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 更新换货物流信息
     */
    @PutMapping("/{id}/logistics/replacement")
    @Operation(summary = "更新换货物流信息", description = "更新换货物流单号和物流公司")
    @PreAuthorize("hasPermission('purchase_return', 'update')")
    public ResponseEntity<Map<String, Object>> updateReplacementLogistics(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam String logisticsCompany) {
        boolean success = purchaseReturnService.updateReplacementLogistics(id, trackingNumber, logisticsCompany);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "换货物流信息更新成功" : "换货物流信息更新失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 标记退货完成
     */
    @PostMapping("/{id}/return/complete")
    @Operation(summary = "标记退货完成", description = "标记退货流程已完成")
    @PreAuthorize("hasPermission('purchase_return', 'update')")
    public ResponseEntity<Map<String, Object>> markReturnComplete(
            @PathVariable Long id,
            @RequestParam BigDecimal returnQuantity) {
        boolean success = purchaseReturnService.markReturnComplete(id, returnQuantity);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "退货完成标记成功" : "退货完成标记失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 标记换货完成
     */
    @PostMapping("/{id}/replacement/complete")
    @Operation(summary = "标记换货完成", description = "标记换货流程已完成")
    @PreAuthorize("hasPermission('purchase_return', 'update')")
    public ResponseEntity<Map<String, Object>> markReplacementComplete(
            @PathVariable Long id,
            @RequestParam BigDecimal replacedQuantity) {
        boolean success = purchaseReturnService.markReplacementComplete(id, replacedQuantity);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "换货完成标记成功" : "换货完成标记失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 完成换货单
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成换货单", description = "完成整个换货流程")
    @PreAuthorize("hasPermission('purchase_return', 'complete')")
    public ResponseEntity<Map<String, Object>> complete(
            @PathVariable Long id,
            @RequestParam(required = false) String completionNote) {
        boolean success = purchaseReturnService.complete(id, completionNote);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "换货单完成成功" : "换货单完成失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 取消换货单
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消换货单", description = "取消换货单流程")
    @PreAuthorize("hasPermission('purchase_return', 'cancel')")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable Long id,
            @RequestParam String cancellationReason) {
        boolean success = purchaseReturnService.cancel(id, cancellationReason);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "换货单取消成功" : "换货单取消失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 计算换货成本
     */
    @GetMapping("/{id}/cost")
    @Operation(summary = "计算换货成本", description = "计算指定换货单的总成本")
    @PreAuthorize("hasPermission('purchase_return', 'read')")
    public ResponseEntity<Map<String, Object>> calculateReturnCost(@PathVariable Long id) {
        BigDecimal cost = purchaseReturnService.calculateReturnCost(id);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "成本计算成功",
            "data", Map.of("totalCost", cost)
        ));
    }
    
    /**
     * 更新索赔信息
     */
    @PutMapping("/{id}/claim")
    @Operation(summary = "更新索赔信息", description = "更新换货单的索赔信息")
    @PreAuthorize("hasPermission('purchase_return', 'update')")
    public ResponseEntity<Map<String, Object>> updateClaim(
            @PathVariable Long id,
            @RequestParam BigDecimal claimAmount,
            @RequestParam String claimStatus,
            @RequestParam(required = false) String claimNote) {
        boolean success = purchaseReturnService.updateClaim(id, claimAmount, claimStatus, claimNote);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", success ? "索赔信息更新成功" : "索赔信息更新失败",
            "data", Map.of("success", success)
        ));
    }
    
    /**
     * 根据采购订单查询换货单列表
     */
    @GetMapping("/order/{purchaseOrderCode}")
    @Operation(summary = "根据采购订单查询换货单", description = "根据采购订单号查询相关的换货单列表")
    @PreAuthorize("hasPermission('purchase_return', 'read')")
    public ResponseEntity<Map<String, Object>> getByPurchaseOrder(
            @PathVariable String purchaseOrderCode) {
        List<PurchaseReturnDTO> returns = purchaseReturnService.getByPurchaseOrder(purchaseOrderCode);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "查询成功",
            "data", returns
        ));
    }
    
    /**
     * 获取待审批列表
     */
    @GetMapping("/pending/approval")
    @Operation(summary = "获取待审批列表", description = "获取当前用户需要审批的换货单列表")
    @PreAuthorize("hasPermission('purchase_return', 'approve')")
    public ResponseEntity<Map<String, Object>> getPendingApprovalList(
            @RequestParam Long userId,
            @RequestParam String role) {
        List<PurchaseReturnDTO> pendingList = purchaseReturnService.getPendingApprovalList(userId, role);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "查询成功",
            "data", pendingList
        ));
    }
    
    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取换货单统计信息", description = "获取换货单的统计分析数据")
    @PreAuthorize("hasPermission('purchase_return', 'read')")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String returnType) {
        PurchaseReturnService.ReturnStatisticsDTO statistics = 
            purchaseReturnService.getStatistics(startDate, endDate, supplierId, returnType);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "统计查询成功",
            "data", statistics
        ));
    }
    
    /**
     * 导出换货单数据
     */
    @GetMapping("/export")
    @Operation(summary = "导出换货单数据", description = "导出换货单数据到Excel、PDF或CSV格式")
    @PreAuthorize("hasPermission('purchase_return', 'export')")
    public ResponseEntity<Map<String, Object>> exportReturns(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "excel") String exportType) {
        String filePath = purchaseReturnService.exportReturns(startDate, endDate, exportType);
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "导出成功",
            "data", Map.of("filePath", filePath)
        ));
    }
    
    /**
     * 检查换货单状态
     */
    @GetMapping("/{id}/status")
    @Operation(summary = "检查换货单状态", description = "检查换货单的当前状态和进度")
    @PreAuthorize("hasPermission('purchase_return', 'read')")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable Long id) {
        PurchaseReturn purchaseReturn = purchaseReturnService.getById(id);
        if (purchaseReturn == null) {
            return ResponseEntity.ok(Map.of(
                "code", 404,
                "message", "换货单不存在",
                "data", null
            ));
        }
        
        Map<String, Object> statusInfo = Map.of(
            "returnCode", purchaseReturn.getReturnCode(),
            "status", purchaseReturn.getStatus(),
            "currentApprovalNode", purchaseReturn.getCurrentApprovalNode(),
            "supplierConfirmationStatus", purchaseReturn.getSupplierConfirmationStatus(),
            "processInstanceId", purchaseReturn.getProcessInstanceId(),
            "lastUpdated", purchaseReturn.getUpdatedAt()
        );
        
        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "状态查询成功",
            "data", statusInfo
        ));
    }
}