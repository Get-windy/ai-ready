package cn.aiedge.erp.sale.retail.controller;

import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailOrderItem;
import cn.aiedge.erp.sale.retail.service.IRetailOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "零售单管理")
@RestController
@RequestMapping("/api/sales/retail")
@RequiredArgsConstructor
public class RetailController {

    private final IRetailOrderService retailOrderService;

    @Operation(summary = "分页查询零售单")
    @GetMapping("/page")
    public Map<String, Object> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(name = "orderNo", required = false) String retailNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Page<RetailOrder> p = new Page<>(page, size);
        IPage<RetailOrder> result = retailOrderService.pageRetails(p,
                retailNo, customerName, startDate, endDate);

        return Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "current", result.getCurrent(),
                "size", result.getSize()
        );
    }

    @Operation(summary = "查询零售单详情（含明细行）")
    @GetMapping("/{id}")
    public IRetailOrderService.RetailOrderDetailVO getDetail(@PathVariable Long id) {
        return retailOrderService.getDetailById(id);
    }

    @Operation(summary = "新增零售单（含明细行）")
    @PostMapping
    public RetailOrder create(@RequestBody Map<String, Object> body) {
        RetailOrder order = parseOrder(body);
        List<RetailOrderItem> items = parseItems(body);
        return retailOrderService.saveWithItems(order, items);
    }

    @Operation(summary = "更新零售单（含明细行）")
    @PutMapping("/{id}")
    public RetailOrder update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        RetailOrder order = parseOrder(body);
        order.setId(id);
        List<RetailOrderItem> items = parseItems(body);
        return retailOrderService.updateWithItems(order, items);
    }

    @Operation(summary = "删除零售单")
    @DeleteMapping("/{id}")
    public Boolean delete(@PathVariable Long id) {
        return retailOrderService.removeById(id);
    }

    @Operation(summary = "查询零售单明细行")
    @GetMapping("/{id}/items")
    public List<RetailOrderItem> listItems(@PathVariable Long id) {
        return retailOrderService.listItemsByOrderId(id);
    }

    // ── 内部工具方法 ──

    @SuppressWarnings("unchecked")
    private RetailOrder parseOrder(Map<String, Object> body) {
        RetailOrder order = new RetailOrder();
        if (body.get("retailNo") != null) order.setRetailNo((String) body.get("retailNo"));
        if (body.get("customerId") != null) order.setCustomerId(toLong(body.get("customerId")));
        if (body.get("customerName") != null) order.setCustomerName((String) body.get("customerName"));
        if (body.get("paymentMethod") != null) order.setPaymentMethod((String) body.get("paymentMethod"));
        if (body.get("status") != null) order.setStatus(toInt(body.get("status")));
        if (body.get("remark") != null) order.setRemark((String) body.get("remark"));
        if (body.get("warehouseId") != null) order.setWarehouseId(toLong(body.get("warehouseId")));
        if (body.get("warehouseName") != null) order.setWarehouseName((String) body.get("warehouseName"));
        if (body.get("handlerId") != null) order.setHandlerId(toLong(body.get("handlerId")));
        if (body.get("handlerName") != null) order.setHandlerName((String) body.get("handlerName"));
        if (body.get("orderDate") != null) order.setOrderDate(java.time.LocalDate.parse((String) body.get("orderDate")));
        if (body.get("saleType") != null) order.setSaleType((String) body.get("saleType"));
        if (body.get("memberCardNo") != null) order.setMemberCardNo((String) body.get("memberCardNo"));
        if (body.get("memberName") != null) order.setMemberName((String) body.get("memberName"));
        if (body.get("directDiscount") != null) order.setDirectDiscount(toBigDecimal(body.get("directDiscount")));
        if (body.get("couponDiscount") != null) order.setCouponDiscount(toBigDecimal(body.get("couponDiscount")));
        if (body.get("promoDiscount") != null) order.setPromoDiscount(toBigDecimal(body.get("promoDiscount")));
        if (body.get("prevPoints") != null) order.setPrevPoints(toInt(body.get("prevPoints")));
        if (body.get("payableAmount") != null) order.setPayableAmount(toBigDecimal(body.get("payableAmount")));
        if (body.get("cashAmount") != null) order.setCashAmount(toBigDecimal(body.get("cashAmount")));
        if (body.get("cardAmount") != null) order.setCardAmount(toBigDecimal(body.get("cardAmount")));
        if (body.get("prepaidAmount") != null) order.setPrepaidAmount(toBigDecimal(body.get("prepaidAmount")));
        if (body.get("transferAmount") != null) order.setTransferAmount(toBigDecimal(body.get("transferAmount")));
        if (body.get("combinedPayment") != null) order.setCombinedPayment(Boolean.TRUE.equals(body.get("combinedPayment")));
        if (body.get("changeAmount") != null) order.setChangeAmount(toBigDecimal(body.get("changeAmount")));
        if (body.get("prepaidBalance") != null) order.setPrepaidBalance(toBigDecimal(body.get("prepaidBalance")));
        return order;
    }

    @SuppressWarnings("unchecked")
    private List<RetailOrderItem> parseItems(Map<String, Object> body) {
        Object itemsObj = body.get("items");
        if (!(itemsObj instanceof List)) return List.of();
        return ((List<Map<String, Object>>) itemsObj).stream().map(m -> {
            RetailOrderItem item = new RetailOrderItem();
            if (m.get("productId") != null) item.setProductId(toLong(m.get("productId")));
            if (m.get("productName") != null) item.setProductName((String) m.get("productName"));
            if (m.get("itemCode") != null) item.setItemCode((String) m.get("itemCode"));
            if (m.get("barcode") != null) item.setBarcode((String) m.get("barcode"));
            if (m.get("unit") != null) item.setUnit((String) m.get("unit"));
            if (m.get("batchCode") != null) item.setBatchCode((String) m.get("batchCode"));
            if (m.get("quantity") != null) item.setQuantity(toBigDecimal(m.get("quantity")));
            if (m.get("unitPrice") != null) item.setUnitPrice(toBigDecimal(m.get("unitPrice")));
            if (m.get("bigPack") != null) item.setBigPack(toInt(m.get("bigPack")));
            if (m.get("midPack") != null) item.setMidPack(toInt(m.get("midPack")));
            if (m.get("smallPack") != null) item.setSmallPack(toInt(m.get("smallPack")));
            if (m.get("remark") != null) item.setRemark((String) m.get("remark"));
            return item;
        }).collect(java.util.stream.Collectors.toList());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }

    private java.math.BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return new java.math.BigDecimal(v.toString());
        try { return new java.math.BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }
}
