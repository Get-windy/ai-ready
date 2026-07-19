package cn.aiedge.erp.sale.salereturn.controller;

import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturnItem;
import cn.aiedge.erp.sale.salereturn.service.SaleReturnService;
import cn.aiedge.common.result.ApiResponse;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/erp/sale/return")
@RequiredArgsConstructor
@Tag(name = "销售退货申请管理", description = "销售退货申请单的创建、审批、执行等操作")
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    @GetMapping("/page")
    @Operation(summary = "分页查询退货申请单")
    @SaCheckLogin
    public ApiResponse<Page<SaleReturn>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String handlerName,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String itemRemark,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String generateType,
            @RequestParam(required = false) String settleStatus,
            @RequestParam(required = false) Integer printCount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String auditorName,
            @RequestParam(required = false) String submitBy,
            @RequestParam(required = false) String productLineAttr,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) Integer extNum1,
            @RequestParam(required = false) Integer extNum2,
            @RequestParam(required = false) String extText1,
            @RequestParam(required = false) String extText2,
            @RequestParam(required = false) String extText3,
            @RequestParam(required = false) String contactName,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String contactAddress,
            @RequestParam(required = false) String auditTime,
            @RequestParam(required = false) String salesType) {
        Page<SaleReturn> result = saleReturnService.pageList(pageNum, pageSize, keyword, customerName,
                handlerName, deptName, warehouseName, productName, itemRemark, status,
                generateType, settleStatus, printCount, startDate, endDate, categoryId,
                creatorName, auditorName, submitBy, productLineAttr, remark, summary,
                deliveryMethod, extNum1, extNum2, extText1, extText2, extText3,
                contactName, contactPhone, contactAddress, auditTime, salesType);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取退货申请单详情")
    @SaCheckLogin
    public ApiResponse<SaleReturn> getById(@PathVariable Long id) {
        SaleReturn data = saleReturnService.getByIdWithItems(id);
        return ApiResponse.ok(data);
    }

    @GetMapping("/returnNo/{returnNo}")
    @Operation(summary = "根据退货单号获取退货申请单")
    @SaCheckLogin
    public ApiResponse<SaleReturn> getByReturnNo(@PathVariable String returnNo) {
        SaleReturn data = saleReturnService.getByReturnNo(returnNo);
        return ApiResponse.ok(data);
    }

    @PostMapping
    @Operation(summary = "创建退货申请单")
    @SaCheckLogin
    public ApiResponse<SaleReturn> create(@RequestBody SaleReturn returnOrder) {
        SaleReturn result = saleReturnService.createReturn(returnOrder);
        return ApiResponse.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新退货申请单")
    @SaCheckLogin
    public ApiResponse<SaleReturn> update(@PathVariable Long id, @RequestBody SaleReturn returnOrder) {
        returnOrder.setId(id);
        SaleReturn result = saleReturnService.updateReturn(returnOrder);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    @SaCheckLogin
    public ApiResponse<SaleReturn> submit(@PathVariable Long id) {
        SaleReturn result = saleReturnService.submitForApproval(id);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批退货申请单")
    @SaCheckLogin
    public ApiResponse<SaleReturn> approve(
            @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String note) {
        SaleReturn result = saleReturnService.approve(id, note);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝退货申请单")
    @SaCheckLogin
    public ApiResponse<SaleReturn> reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String reason) {
        SaleReturn result = saleReturnService.reject(id, reason);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成退货申请单")
    @SaCheckLogin
    public ApiResponse<SaleReturn> complete(@PathVariable Long id) {
        SaleReturn result = saleReturnService.complete(id);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消退货申请单")
    @SaCheckLogin
    public ApiResponse<SaleReturn> cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        SaleReturn result = saleReturnService.cancel(id, reason);
        return ApiResponse.ok(result);
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量审批退货申请单")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> batchApprove(
            @RequestBody List<Long> ids,
            @Parameter(description = "审批意见") @RequestParam(required = false) String note) {
        int count = saleReturnService.batchApprove(ids, note);
        Map<String, Object> result = Map.of("approved", count, "total", ids.size());
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取退货申请单明细")
    @SaCheckLogin
    public ApiResponse<List<SaleReturnItem>> getItems(@PathVariable Long id) {
        List<SaleReturnItem> items = saleReturnService.getItems(id);
        return ApiResponse.ok(items);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除退货申请单")
    @SaCheckLogin
    public ApiResponse<Void> delete(@PathVariable Long id) {
        saleReturnService.removeById(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除退货申请单")
    @SaCheckLogin
    public ApiResponse<Boolean> batchDelete(@RequestBody List<Long> ids) {
        boolean result = saleReturnService.removeBatchByIds(ids);
        return ApiResponse.ok(result);
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询退货申请单明细（按明细）")
    @SaCheckLogin
    public ApiResponse<Page<Map<String, Object>>> pageDetail(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "退货单号") @RequestParam(required = false) String returnNo,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "经手人ID") @RequestParam(required = false) Long handlerId,
            @Parameter(description = "部门名称") @RequestParam(required = false) String deptName,
            @Parameter(description = "结算状态") @RequestParam(required = false) String settleStatus,
            @Parameter(description = "销售类型") @RequestParam(required = false) String salesType,
            @Parameter(description = "商品行属性") @RequestParam(required = false) String productLineAttr,
            @Parameter(description = "明细备注") @RequestParam(required = false) String itemRemark,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "审核人") @RequestParam(required = false) String auditorName,
            @Parameter(description = "单据备注") @RequestParam(required = false) String remark,
            @Parameter(description = "是否赠品") @RequestParam(required = false) Boolean isGift,
            @Parameter(description = "审核时间") @RequestParam(required = false) String auditTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Page<Map<String, Object>> result = saleReturnService.pageDetail(
                keyword, customerId, warehouseId, status, returnNo, productName,
                handlerId, deptName, settleStatus, salesType, productLineAttr,
                itemRemark, startDate, endDate, pageNum, pageSize,
                creatorName, auditorName, remark, isGift, auditTime);
        return ApiResponse.ok(result);
    }

    @GetMapping("/export")
    @Operation(summary = "导出退货申请单列表")
    @SaCheckLogin
    public ApiResponse<List<SaleReturn>> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户名称") @RequestParam(required = false) String customerName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        List<SaleReturn> result = saleReturnService.exportList(keyword, customerName, status);
        return ApiResponse.ok(result);
    }
}
