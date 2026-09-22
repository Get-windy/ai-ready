package cn.aiedge.erp.sale.retail.controller;

import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailOrderItem;
import cn.aiedge.erp.sale.retail.entity.RetailOrderPayment;
import cn.aiedge.erp.sale.retail.service.IRetailOrderService;
import cn.aiedge.erp.sale.retail.service.IRetailOrderService.RetailOrderDetailVO;
import cn.aiedge.erp.sale.retail.service.IRetailOrderService.RetailQueryDTO;
import cn.aiedge.erp.sale.retail.service.IRetailOrderService.RetailDetailQueryDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "零售单管理", description = "零售单创建、结算、挂单、作废等操作")
@RestController
@RequestMapping("/api/sales/retail")
@RequiredArgsConstructor
public class RetailController {

    private final IRetailOrderService retailOrderService;

    // ═══ 1. 按单据分页查询 ═══
    @SaCheckPermission("sale:retail:list")
    @GetMapping("/page/doc")
    @Operation(summary = "按单据分页查询零售单")
    public IPage<RetailOrder> pageByDoc(
            @Parameter(description = "开始日期") @RequestParam(required = false) String dateStart,
            @Parameter(description = "结束日期") @RequestParam(required = false) String dateEnd,
            @Parameter(description = "零售单号") @RequestParam(required = false) String retailNo,
            @Parameter(description = "客户名称") @RequestParam(required = false) String customerName,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "经手人ID") @RequestParam(required = false) Long handlerId,
            @Parameter(description = "经手人姓名") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "部门名称") @RequestParam(required = false) String departmentName,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "仓库名称") @RequestParam(required = false) String warehouseName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "销售类型") @RequestParam(required = false) String saleType,
            @Parameter(description = "商品行属性") @RequestParam(required = false) String productAttribute,
            @Parameter(description = "单据备注") @RequestParam(required = false) String remark,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "记账人") @RequestParam(required = false) String bookkeeperName,
            @Parameter(description = "会员卡号") @RequestParam(required = false) String memberCardNo,
            @Parameter(description = "打印次数") @RequestParam(required = false) Integer printCount,
            @Parameter(description = "表头自定义字段1(数字)") @RequestParam(required = false) java.math.BigDecimal extNum1,
            @Parameter(description = "表头自定义字段2(数字)") @RequestParam(required = false) java.math.BigDecimal extNum2,
            @Parameter(description = "表头自定义字段3(文本)") @RequestParam(required = false) String extText1,
            @Parameter(description = "表头自定义字段4(文本)") @RequestParam(required = false) String extText2,
            @Parameter(description = "表头自定义字段5(文本)") @RequestParam(required = false) String extText3,
            @Parameter(description = "显示红冲") @RequestParam(required = false) Boolean showRedFlush,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        RetailQueryDTO query = new RetailQueryDTO();
        query.setDateStart(dateStart);
        query.setDateEnd(dateEnd);
        query.setRetailNo(retailNo);
        query.setCustomerName(customerName);
        query.setCustomerId(customerId);
        query.setHandlerId(handlerId);
        query.setHandlerName(handlerName);
        query.setDepartmentId(departmentId);
        query.setDepartmentName(departmentName);
        query.setWarehouseId(warehouseId);
        query.setWarehouseName(warehouseName);
        query.setStatus(status);
        query.setSaleType(saleType);
        query.setProductAttribute(productAttribute);
        query.setRemark(remark);
        query.setCreatorName(creatorName);
        query.setBookkeeperName(bookkeeperName);
        query.setMemberCardNo(memberCardNo);
        query.setPrintCount(printCount);
        query.setExtNum1(extNum1);
        query.setExtNum2(extNum2);
        query.setExtText1(extText1);
        query.setExtText2(extText2);
        query.setExtText3(extText3);
        query.setShowRedFlush(showRedFlush);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return retailOrderService.pageByDoc(new Page<>(pageNum, pageSize), query);
    }

    // ═══ 2. 按明细分页查询 ═══
    @SaCheckPermission("sale:retail:list")
    @GetMapping("/page/detail")
    @Operation(summary = "按明细分页查询零售单")
    public IPage<Map<String, Object>> pageByDetail(
            @Parameter(description = "开始日期") @RequestParam(required = false) String dateStart,
            @Parameter(description = "结束日期") @RequestParam(required = false) String dateEnd,
            @Parameter(description = "零售单号") @RequestParam(required = false) String retailNo,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "条码") @RequestParam(required = false) String barcode,
            @Parameter(description = "客户名称") @RequestParam(required = false) String customerName,
            @Parameter(description = "经手人ID") @RequestParam(required = false) Long handlerId,
            @Parameter(description = "经手人姓名") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "部门名称") @RequestParam(required = false) String departmentName,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "仓库名称") @RequestParam(required = false) String warehouseName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "明细备注") @RequestParam(required = false) String remark,
            @Parameter(description = "显示红冲") @RequestParam(required = false) Boolean showRedFlush,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        RetailDetailQueryDTO query = new RetailDetailQueryDTO();
        query.setDateStart(dateStart);
        query.setDateEnd(dateEnd);
        query.setRetailNo(retailNo);
        query.setProductName(productName);
        query.setBarcode(barcode);
        query.setCustomerName(customerName);
        query.setHandlerId(handlerId);
        query.setHandlerName(handlerName);
        query.setDepartmentId(departmentId);
        query.setDepartmentName(departmentName);
        query.setWarehouseId(warehouseId);
        query.setWarehouseName(warehouseName);
        query.setStatus(status);
        query.setRemark(remark);
        query.setShowRedFlush(showRedFlush);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return retailOrderService.pageByDetail(new Page<>(pageNum, pageSize), query);
    }

    // ═══ 3. 生成下一个零售单号（后端号段） ═══
    @SaCheckPermission("sale:retail:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成下一个零售单号（后端号段）")
    public String nextNo() {
        return retailOrderService.generateRetailNo();
    }

    // ═══ 4. 查询详情（含明细行） ═══
    @SaCheckPermission("sale:retail:detail")
    @GetMapping("/{id}")
    @Operation(summary = "查询零售单详情（含明细行和支付明细）")
    public RetailOrderDetailVO getDetail(@PathVariable Long id) {
        return retailOrderService.getDetailById(id);
    }

    // ═══ 4. 挂单列表 ═══
    @SaCheckPermission("sale:retail:view")
    @GetMapping("/hold-list")
    @Operation(summary = "查询挂单列表")
    public List<RetailOrder> holdList(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        return retailOrderService.listHoldOrders(warehouseId);
    }

    // ═══ 5. 创建零售单 ═══
    @SaCheckPermission("sale:retail:create")
    @PostMapping
    @Operation(summary = "创建零售单（含明细行）")
    public RetailOrder create(@RequestBody RetailOrderCreateRequest request) {
        RetailOrder order = request.getOrder();
        List<RetailOrderItem> items = request.getItems();
        return retailOrderService.saveWithItems(order, items);
    }

    // ═══ 6. 更新零售单（草稿状态） ═══
    @SaCheckPermission("sale:retail:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新零售单（含明细行）")
    public RetailOrder update(@PathVariable Long id, @RequestBody RetailOrderCreateRequest request) {
        RetailOrder order = request.getOrder();
        order.setId(id);
        List<RetailOrderItem> items = request.getItems();
        return retailOrderService.updateWithItems(order, items);
    }

    // ═══ 7. 复制单据 ═══
    @SaCheckPermission("sale:retail:create")
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制零售单")
    public RetailOrder copy(@PathVariable Long id) {
        return retailOrderService.copyOrder(id);
    }

    // ═══ 8. 结算 ═══
    @SaCheckPermission("sale:retail:update")
    @PostMapping("/{id}/settle")
    @Operation(summary = "结算零售单")
    public RetailOrder settle(@PathVariable Long id, @RequestBody SettleRequest request) {
        return retailOrderService.settle(id, request.getPayments());
    }

    // ═══ 9. 挂单 ═══
    @SaCheckPermission("sale:retail:update")
    @PostMapping("/{id}/hold")
    @Operation(summary = "挂单")
    public void hold(@PathVariable Long id) {
        retailOrderService.hold(id);
    }

    // ═══ 10. 取单 ═══
    @SaCheckPermission("sale:retail:update")
    @PostMapping("/{id}/unhold")
    @Operation(summary = "取单")
    public void unhold(@PathVariable Long id) {
        retailOrderService.unhold(id);
    }

    // ═══ 11. 作废 ═══
    @SaCheckPermission("sale:retail:update")
    @PostMapping("/{id}/void")
    @Operation(summary = "作废零售单")
    public void voidOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        retailOrderService.voidOrder(id, reason);
    }

    // ═══ 12. 打印数据 ═══
    @SaCheckPermission("sale:retail:view")
    @GetMapping("/{id}/print-data")
    @Operation(summary = "获取打印数据")
    public RetailOrderDetailVO printData(@PathVariable Long id) {
        return retailOrderService.getDetailById(id);
    }

    // ═══ 13. 打印后更新计数 ═══
    @SaCheckPermission("sale:retail:print")
    @PostMapping("/{id}/print")
    @Operation(summary = "打印后更新打印次数")
    public void afterPrint(@PathVariable Long id) {
        retailOrderService.incrementPrintCount(id);
    }

    // ═══ 14. 商品快速查找 ═══
    @SaCheckPermission("sale:retail:view")
    @GetMapping("/products/quick")
    @Operation(summary = "商品快速查找")
    public List<Map<String, Object>> quickSearchProducts(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        return retailOrderService.quickSearchProducts(keyword, warehouseId);
    }

    // ═══ 15. 查询明细行 ═══
    @SaCheckPermission("sale:retail:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "查询零售单明细行")
    public List<RetailOrderItem> listItems(@PathVariable Long id) {
        return retailOrderService.listItemsByOrderId(id);
    }

    // ═══ 16. 删除零售单 ═══
    @SaCheckPermission("sale:retail:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除零售单（仅草稿状态）")
    public void delete(@PathVariable Long id) {
        retailOrderService.deleteOrder(id);
    }

    // ── 请求体DTO ──

    public static class RetailOrderCreateRequest {
        private RetailOrder order;
        private List<RetailOrderItem> items;

        public RetailOrder getOrder() { return order; }
        public void setOrder(RetailOrder order) { this.order = order; }
        public List<RetailOrderItem> getItems() { return items; }
        public void setItems(List<RetailOrderItem> items) { this.items = items; }
    }

    public static class SettleRequest {
        private List<RetailOrderPayment> payments;

        public List<RetailOrderPayment> getPayments() { return payments; }
        public void setPayments(List<RetailOrderPayment> payments) { this.payments = payments; }
    }
}
