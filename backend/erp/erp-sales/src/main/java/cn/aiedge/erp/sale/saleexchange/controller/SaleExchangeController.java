package cn.aiedge.erp.sale.saleexchange.controller;

import cn.aiedge.erp.sale.saleexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import cn.aiedge.erp.sale.saleexchange.service.SaleExchangeService;
import cn.dev33.satoken.stp.StpUtil;
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
@RequestMapping("/api/erp/sale/exchange")
@RequiredArgsConstructor
@Tag(name = "销售换货管理", description = "销售换货单的创建、审批、执行等操作")
public class SaleExchangeController {

    private final SaleExchangeService saleExchangeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询换货单（扩展版）")
    public Page<SaleExchange> page(
            @Parameter(description = "查询参数") @RequestParam Map<String, Object> params,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return saleExchangeService.pageListExtended(params, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取换货单详情")
    public SaleExchange getById(@PathVariable Long id) {
        SaleExchange exchange = saleExchangeService.getById(id);
        if (exchange != null) {
            // 加载明细
            List<SaleExchangeItem> items = saleExchangeService.getItems(id);
            exchange.setItems(items);
        }
        return exchange;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取换货单明细")
    public List<SaleExchangeItem> getItems(@PathVariable Long id) {
        return saleExchangeService.getItems(id);
    }

    @GetMapping("/{id}/items/{warehouseType}")
    @Operation(summary = "按仓库类型获取明细")
    public List<SaleExchangeItem> getItemsByWarehouseType(
            @PathVariable Long id,
            @PathVariable Integer warehouseType) {
        return saleExchangeService.getItemsByWarehouseType(id, warehouseType);
    }

    @PostMapping
    @Operation(summary = "创建换货单")
    public SaleExchange create(@RequestBody Map<String, Object> data) {
        SaleExchange exchange = convertToExchange(data);
        List<SaleExchangeItem> items = convertToItems(data);
        return saleExchangeService.createExchange(exchange, items);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新换货单")
    public SaleExchange update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        SaleExchange exchange = convertToExchange(data);
        List<SaleExchangeItem> items = convertToItems(data);
        return saleExchangeService.updateExchange(id, exchange, items);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除换货单")
    public void delete(@PathVariable Long id) {
        saleExchangeService.removeById(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除换货单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return saleExchangeService.removeBatchByIds(ids);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public SaleExchange submit(@PathVariable Long id) {
        return saleExchangeService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批换货单")
    public SaleExchange approve(
            @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String remark) {
        Long approverId = StpUtil.getLoginIdAsLong();
        String approvedByName = StpUtil.getLoginIdAsString();
        return saleExchangeService.approve(id, approverId, approvedByName, remark);
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量审核")
    public int batchApprove(@RequestBody List<Long> ids) {
        Long approverId = StpUtil.getLoginIdAsLong();
        String approvedByName = StpUtil.getLoginIdAsString();
        return saleExchangeService.batchApprove(ids, approverId, approvedByName);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝换货单")
    public SaleExchange reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String remark) {
        return saleExchangeService.reject(id, remark);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消换货单")
    public SaleExchange cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return saleExchangeService.cancel(id, reason);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成换货单")
    public SaleExchange complete(@PathVariable Long id) {
        return saleExchangeService.complete(id);
    }

    @PostMapping("/{id}/print")
    @Operation(summary = "打印（打印次数+1）")
    public void print(@PathVariable Long id) {
        saleExchangeService.print(id);
    }

    @PostMapping("/batch-print")
    @Operation(summary = "批量打印")
    public void batchPrint(@RequestBody List<Long> ids) {
        saleExchangeService.batchPrint(ids);
    }

    @GetMapping("/{id}/approval-records")
    @Operation(summary = "获取审批记录")
    public List<ExchangeApprovalRecord> getApprovalRecords(@PathVariable Long id) {
        return saleExchangeService.getApprovalRecords(id);
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "获取换货单跟踪信息")
    public Map<String, Object> getTracking(@PathVariable Long id) {
        return saleExchangeService.getTracking(id);
    }

    @GetMapping("/export")
    @Operation(summary = "导出换货单列表")
    public List<SaleExchange> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "换货类型") @RequestParam(required = false) Integer exchangeType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        return saleExchangeService.exportList(keyword, customerId, status, exchangeType, startDate, endDate);
    }

    // ========== 辅助方法 ==========

    private SaleExchange convertToExchange(Map<String, Object> data) {
        SaleExchange exchange = new SaleExchange();
        if (data.containsKey("id")) {
            exchange.setId(Long.valueOf(data.get("id").toString()));
        }
        if (data.containsKey("customerId")) {
            exchange.setCustomerId(Long.valueOf(data.get("customerId").toString()));
        }
        if (data.containsKey("customerName")) {
            exchange.setCustomerName((String) data.get("customerName"));
        }
        if (data.containsKey("customerCode")) {
            exchange.setCustomerCode((String) data.get("customerCode"));
        }
        if (data.containsKey("exchangeDate")) {
            exchange.setExchangeDate(java.time.LocalDateTime.parse(data.get("exchangeDate").toString()));
        }
        if (data.containsKey("inWarehouseId")) {
            exchange.setInWarehouseId(Long.valueOf(data.get("inWarehouseId").toString()));
        }
        if (data.containsKey("inWarehouseName")) {
            exchange.setInWarehouseName((String) data.get("inWarehouseName"));
        }
        if (data.containsKey("outWarehouseId")) {
            exchange.setOutWarehouseId(Long.valueOf(data.get("outWarehouseId").toString()));
        }
        if (data.containsKey("outWarehouseName")) {
            exchange.setOutWarehouseName((String) data.get("outWarehouseName"));
        }
        if (data.containsKey("handlerId")) {
            exchange.setHandlerId(Long.valueOf(data.get("handlerId").toString()));
        }
        if (data.containsKey("handlerName")) {
            exchange.setHandlerName((String) data.get("handlerName"));
        }
        if (data.containsKey("salesType")) {
            exchange.setSalesType((String) data.get("salesType"));
        }
        if (data.containsKey("remark")) {
            exchange.setRemark((String) data.get("remark"));
        }
        return exchange;
    }

    private List<SaleExchangeItem> convertToItems(Map<String, Object> data) {
        // 从请求体中提取明细数据
        if (data.containsKey("items")) {
            Object itemsObj = data.get("items");
            if (itemsObj instanceof List) {
                List<?> itemsList = (List<?>) itemsObj;
                List<SaleExchangeItem> items = new java.util.ArrayList<>();
                for (Object itemObj : itemsList) {
                    if (itemObj instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) itemObj;
                        SaleExchangeItem item = new SaleExchangeItem();
                        if (itemMap.containsKey("productId")) {
                            item.setProductId(Long.valueOf(itemMap.get("productId").toString()));
                        }
                        if (itemMap.containsKey("productName")) {
                            item.setProductName((String) itemMap.get("productName"));
                        }
                        if (itemMap.containsKey("quantity")) {
                            item.setQuantity(new java.math.BigDecimal(itemMap.get("quantity").toString()));
                        }
                        if (itemMap.containsKey("unitPrice")) {
                            item.setUnitPrice(new java.math.BigDecimal(itemMap.get("unitPrice").toString()));
                        }
                        if (itemMap.containsKey("warehouseType")) {
                            item.setWarehouseType(Integer.valueOf(itemMap.get("warehouseType").toString()));
                        }
                        items.add(item);
                    }
                }
                return items;
            }
        }
        return null;
    }
}
