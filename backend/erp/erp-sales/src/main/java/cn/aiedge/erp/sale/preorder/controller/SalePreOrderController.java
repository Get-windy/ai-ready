package cn.aiedge.erp.sale.preorder.controller;

import cn.aiedge.erp.sale.preorder.entity.SalePreOrder;
import cn.aiedge.erp.sale.preorder.entity.SalePreOrderItem;
import cn.aiedge.erp.sale.preorder.mapper.SalePreOrderItemMapper;
import cn.aiedge.erp.sale.preorder.service.SalePreOrderService;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/erp/sale/pre-order")
@RequiredArgsConstructor
@Tag(name = "预订货单管理", description = "预订货单CRUD、提交审批、导出等操作")
public class SalePreOrderController {

    private final SalePreOrderService salePreOrderService;
    private final SalePreOrderItemMapper itemMapper;
    private final BizNumberGeneratorService bizNumberGeneratorService;

    @GetMapping("/page")
    @Operation(summary = "分页查询预订货单（按单据）")
    @SaCheckLogin
    public Page<SalePreOrder> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户名称") @RequestParam(required = false) String customerName,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String deptName,
            @Parameter(description = "状态(逗号分隔)") @RequestParam(required = false) String status,
            @Parameter(description = "结算状态") @RequestParam(required = false) Integer settlementStatus,
            @Parameter(description = "收款期限起") @RequestParam(required = false) String depositDeadlineStart,
            @Parameter(description = "收款期限止") @RequestParam(required = false) String depositDeadlineEnd,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "仓库") @RequestParam(required = false) String warehouseName,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "审核人") @RequestParam(required = false) String auditorName,
            @Parameter(description = "销售类型") @RequestParam(required = false) Integer saleType,
            @Parameter(description = "单据备注") @RequestParam(required = false) String remark,
            @Parameter(description = "表头自定义1(数字)") @RequestParam(required = false) BigDecimal extNum1,
            @Parameter(description = "表头自定义2(数字)") @RequestParam(required = false) BigDecimal extNum2,
            @Parameter(description = "表头自定义3(文本)") @RequestParam(required = false) String extText1,
            @Parameter(description = "表头自定义4(文本)") @RequestParam(required = false) String extText2,
            @Parameter(description = "表头自定义5(文本)") @RequestParam(required = false) String extText3,
            @Parameter(description = "商品行属性") @RequestParam(required = false) String productAttribute,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {

        Integer[] statusArr = null;
        if (status != null && !status.isEmpty()) {
            statusArr = Arrays.stream(status.split(",")).map(Integer::parseInt).toArray(Integer[]::new);
        }

        return salePreOrderService.pageList(keyword, null, customerName, handlerName, deptName,
                statusArr, settlementStatus, depositDeadlineStart, depositDeadlineEnd,
                startDate, endDate, warehouseName, creatorName, auditorName, saleType, remark,
                extNum1, extNum2, extText1, extText2, extText3, productAttribute,
                pageNum, pageSize);
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询预订货单明细（按明细）")
    @SaCheckLogin
    public Page<Map<String, Object>> pageDetail(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户名称") @RequestParam(required = false) String customerName,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String deptName,
            @Parameter(description = "单据编号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "状态(逗号分隔)") @RequestParam(required = false) String status,
            @Parameter(description = "结算状态") @RequestParam(required = false) Integer settlementStatus,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "审核人") @RequestParam(required = false) String auditorName,
            @Parameter(description = "销售类型") @RequestParam(required = false) Integer saleType,
            @Parameter(description = "商品行属性") @RequestParam(required = false) String productAttribute,
            @Parameter(description = "单据备注") @RequestParam(required = false) String remark,
            @Parameter(description = "明细备注") @RequestParam(required = false) String itemRemark,
            @Parameter(description = "是否赠品") @RequestParam(required = false) Boolean gift,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {

        Integer[] statusArr = null;
        if (status != null && !status.isEmpty()) {
            statusArr = Arrays.stream(status.split(",")).map(Integer::parseInt).toArray(Integer[]::new);
        }

        return salePreOrderService.pageDetail(keyword, null, customerName, handlerName, deptName,
                orderNo, statusArr, settlementStatus, categoryId, startDate, endDate,
                creatorName, auditorName, saleType, productAttribute, remark, itemRemark, gift,
                pageNum, pageSize);
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一预订货单号（号段 YDHD-yyyyMMdd-NNNN）")
    @SaCheckLogin
    public ApiResponse<String> nextNo() {
        return ApiResponse.ok(salePreOrderService.generateOrderNo());
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获取预订货单详情")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> getDetail(@PathVariable Long id) {
        SalePreOrder order = salePreOrderService.getDetail(id);
        if (order == null) return ApiResponse.fail("预订货单不存在");

        // 获取明细行
        LambdaQueryWrapper<SalePreOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(SalePreOrderItem::getOrderId, id)
                .eq(SalePreOrderItem::getDeleted, 0)
                .orderByAsc(SalePreOrderItem::getLineNo);
        List<SalePreOrderItem> items = itemMapper.selectList(itemWrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("customerId", order.getCustomerId());
        result.put("customerName", order.getCustomerName());
        result.put("customerCode", order.getCustomerCode());
        result.put("bankName", order.getBankName());
        result.put("bankAccount", order.getBankAccount());
        result.put("taxNo", order.getTaxNo());
        result.put("warehouseId", order.getWarehouseId());
        result.put("warehouseName", order.getWarehouseName());
        result.put("handlerId", order.getHandlerId());
        result.put("handlerName", order.getHandlerName());
        result.put("deptId", order.getDeptId());
        result.put("deptName", order.getDeptName());
        result.put("orderDate", order.getOrderDate());
        result.put("saleType", order.getSaleType());
        result.put("receiverName", order.getReceiverName());
        result.put("receiverPhone", order.getReceiverPhone());
        result.put("shippingAddress", order.getShippingAddress());
        result.put("customerLevel", order.getCustomerLevel());
        result.put("customerTicket", order.getCustomerTicket());
        result.put("customerRemark", order.getCustomerRemark());
        result.put("status", order.getStatus());
        result.put("totalAmount", order.getTotalAmount());
        result.put("discountedAmount", order.getDiscountedAmount());
        result.put("orderAmount", order.getOrderAmount());
        result.put("settlementStatus", order.getSettlementStatus());
        result.put("receivedDeposit", order.getReceivedDeposit());
        result.put("unreceivedDeposit", order.getUnreceivedDeposit());
        result.put("depositBalance", order.getDepositBalance());
        result.put("depositAccount1", order.getDepositAccount1());
        result.put("depositAccount2", order.getDepositAccount2());
        result.put("depositAccount3", order.getDepositAccount3());
        result.put("depositAccount4", order.getDepositAccount4());
        result.put("depositAmount", order.getDepositAmount());
        result.put("creditLimit", order.getCreditLimit());
        result.put("depositDeadline", order.getDepositDeadline());
        result.put("preOrderQuantity", order.getPreOrderQuantity());
        result.put("orderedQuantity", order.getOrderedQuantity());
        result.put("unOrderedQuantity", order.getUnOrderedQuantity());
        result.put("shippedQuantity", order.getShippedQuantity());
        result.put("unShippedQuantity", order.getUnShippedQuantity());
        result.put("totalWeight", order.getTotalWeight());
        result.put("totalVolume", order.getTotalVolume());
        result.put("region", order.getRegion());
        result.put("summary", order.getSummary());
        result.put("remark", order.getRemark());
        result.put("attachment", order.getAttachment());
        result.put("footerExtText1", order.getFooterExtText1());
        result.put("footerExtText2", order.getFooterExtText2());
        result.put("extNum1", order.getExtNum1());
        result.put("extNum2", order.getExtNum2());
        result.put("extText1", order.getExtText1());
        result.put("extText2", order.getExtText2());
        result.put("extText3", order.getExtText3());
        result.put("creatorName", order.getCreatorName());
        result.put("submitterName", order.getSubmitterName());
        result.put("submitTime", order.getSubmitTime());
        result.put("auditorName", order.getAuditorName());
        result.put("printCount", order.getPrintCount());
        result.put("createTime", order.getCreateTime());
        result.put("items", items);

        return ApiResponse.ok(result);
    }

    @PostMapping
    @Operation(summary = "创建预订货单")
    @SaCheckLogin
    public ApiResponse<SalePreOrder> create(@RequestBody Map<String, Object> body) {
        SalePreOrder order = buildOrderFromMap(body);
        List<SalePreOrderItem> items = buildItemsFromMap(body);

        // 生产级ERP: 自动生成单据编号
        if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
            order.setOrderNo(bizNumberGeneratorService.nextPreOrderNo());
        }

        // 使用验证方法创建（包含外键验证）
        SalePreOrder savedOrder = salePreOrderService.validateAndCreate(order, items);

        return ApiResponse.ok(savedOrder);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新预订货单")
    @SaCheckLogin
    public ApiResponse<SalePreOrder> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SalePreOrder order = buildOrderFromMap(body);
        List<SalePreOrderItem> items = buildItemsFromMap(body);

        // 使用验证方法更新（包含外键验证和状态检查）
        salePreOrderService.validateAndUpdate(id, order, items);

        return ApiResponse.ok(order);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除预订货单")
    @SaCheckLogin
    public ApiResponse<Void> delete(@PathVariable Long id) {
        salePreOrderService.removeById(id);
        // 同时删除明细
        LambdaQueryWrapper<SalePreOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalePreOrderItem::getOrderId, id);
        itemMapper.delete(wrapper);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    @SaCheckLogin
    public ApiResponse<Void> submit(@PathVariable Long id) {
        salePreOrderService.submit(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    @SaCheckLogin
    public ApiResponse<Void> approve(@PathVariable Long id) {
        salePreOrderService.approve(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export")
    @Operation(summary = "导出预订货单")
    @SaCheckLogin
    public void export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "客户名称") @RequestParam(required = false) String customerName,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String deptName,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "结算状态") @RequestParam(required = false) Integer settlementStatus,
            jakarta.servlet.http.HttpServletResponse response) {
        log.info("导出预订货单: keyword={}, startDate={}, endDate={}", keyword, startDate, endDate);

        try {
            // 查询数据（不分页）
            Integer[] statusArr = null;
            if (status != null && !status.isEmpty()) {
                statusArr = Arrays.stream(status.split(",")).map(Integer::parseInt).toArray(Integer[]::new);
            }

            Page<SalePreOrder> page = salePreOrderService.pageList(
                keyword, null, customerName, handlerName, deptName,
                statusArr, settlementStatus, null, null,
                startDate, endDate, null, null, null, null, null,
                null, null, null, null, null, null,
                1, 10000 // 导出时获取所有数据
            );

            // 设置响应头
            response.setContentType("text/csv;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String fileName = java.net.URLEncoder.encode("预订货单_" + System.currentTimeMillis() + ".csv", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            // 写入 BOM
            response.getOutputStream().write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });

            // 写入 CSV 内容
            java.io.PrintWriter writer = response.getWriter();

            // 表头
            writer.println("单据日期,单据编号,单据状态,仓库,往来单位,往来单位编号,客户级别,收货人,联系电话,收货地址,客户一票通,客户备注,经手人,部门,金额,折后金额,本单金额,结算状态,已收预订金,未收预订金,本单预订金余额,预订数量,已订数量,未订数量,已发数量,未发数量,重量(kg),体积(m³),区域,销售类型,单据备注,摘要,附件,提交时间,制单人,提交人,审核人,打印次数");

            // 数据行
            for (SalePreOrder order : page.getRecords()) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    escapeCsv(order.getOrderDate() != null ? order.getOrderDate().toString() : ""),
                    escapeCsv(order.getOrderNo()),
                    escapeCsv(getStatusName(order.getStatus())),
                    escapeCsv(order.getWarehouseName()),
                    escapeCsv(order.getCustomerName()),
                    escapeCsv(order.getCustomerCode()),
                    escapeCsv(order.getCustomerLevel()),
                    escapeCsv(order.getReceiverName()),
                    escapeCsv(order.getReceiverPhone()),
                    escapeCsv(order.getShippingAddress()),
                    escapeCsv(order.getCustomerTicket()),
                    escapeCsv(order.getCustomerRemark()),
                    escapeCsv(order.getHandlerName()),
                    escapeCsv(order.getDeptName()),
                    order.getTotalAmount() != null ? order.getTotalAmount() : "",
                    order.getDiscountedAmount() != null ? order.getDiscountedAmount() : "",
                    order.getOrderAmount() != null ? order.getOrderAmount() : "",
                    escapeCsv(getSettlementStatusName(order.getSettlementStatus())),
                    order.getReceivedDeposit() != null ? order.getReceivedDeposit() : "",
                    order.getUnreceivedDeposit() != null ? order.getUnreceivedDeposit() : "",
                    order.getDepositBalance() != null ? order.getDepositBalance() : "",
                    order.getPreOrderQuantity() != null ? order.getPreOrderQuantity() : "",
                    order.getOrderedQuantity() != null ? order.getOrderedQuantity() : "",
                    order.getUnOrderedQuantity() != null ? order.getUnOrderedQuantity() : "",
                    order.getShippedQuantity() != null ? order.getShippedQuantity() : "",
                    order.getUnShippedQuantity() != null ? order.getUnShippedQuantity() : "",
                    order.getTotalWeight() != null ? order.getTotalWeight() : "",
                    order.getTotalVolume() != null ? order.getTotalVolume() : "",
                    escapeCsv(order.getRegion()),
                    escapeCsv(getSaleTypeName(order.getSaleType())),
                    escapeCsv(order.getRemark()),
                    escapeCsv(order.getSummary()),
                    escapeCsv(order.getAttachment()),
                    order.getSubmitTime() != null ? order.getSubmitTime().toString() : "",
                    escapeCsv(order.getCreatorName()),
                    escapeCsv(order.getSubmitterName()),
                    escapeCsv(order.getAuditorName()),
                    order.getPrintCount() != null ? order.getPrintCount() : ""
                ));
            }

            writer.flush();
        } catch (Exception e) {
            log.error("导出失败", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "草稿";
            case 1: return "审核中";
            case 2: return "待订货";
            case 3: return "部分订货";
            case 4: return "已订货";
            case 5: return "已完成";
            case -1: return "已取消";
            default: return "";
        }
    }

    private String getSettlementStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "未结算";
            case 1: return "部分结算";
            case 2: return "已结算";
            default: return "";
        }
    }

    private String getSaleTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 0: return "正常销售";
            case 1: return "样品销售";
            case 2: return "促销销售";
            default: return "";
        }
    }

    @PostMapping("/batch-order")
    @Operation(summary = "批量订货")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> batchOrder(@RequestBody Map<String, Object> body) {
        // 雪花ID超出 JS 安全整数，前端以字符串回传 → 统一走 toLong 解析（数字/字符串均可）
        Object raw = body.get("ids");
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) {
            return ApiResponse.fail("请选择要订货的预订货单");
        }
        List<?> rawIds = (List<?>) raw;
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        for (Object idObj : rawIds) {
            Long id = toLong(idObj);
            try {
                salePreOrderService.batchOrder(id);
                successCount++;
            } catch (Exception e) {
                log.warn("批量订货失败: id={}", id, e);
                errors.add("ID=" + id + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("errors", errors);
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/print")
    @Operation(summary = "打印计数递增")
    @SaCheckLogin
    public ApiResponse<Void> incrementPrintCount(@PathVariable Long id) {
        salePreOrderService.incrementPrintCount(id);
        return ApiResponse.ok(null);
    }

    // ─── 私有方法 ───

    private SalePreOrder buildOrderFromMap(Map<String, Object> body) {
        SalePreOrder order = new SalePreOrder();
        // tenant_id 由 MyBatis-Plus MetaObjectHandler 自动填充

        // 单号来自后端号段 /next-no（前端原样回传，严禁被丢弃后另生成一个号）
        if (body.get("orderNo") != null) order.setOrderNo(str(body.get("orderNo")));
        if (body.get("customerId") != null) order.setCustomerId(toLong(body.get("customerId")));
        if (body.get("customerName") != null) order.setCustomerName(str(body.get("customerName")));
        if (body.get("customerCode") != null) order.setCustomerCode(str(body.get("customerCode")));
        if (body.get("bankName") != null) order.setBankName(str(body.get("bankName")));
        if (body.get("bankAccount") != null) order.setBankAccount(str(body.get("bankAccount")));
        if (body.get("taxNo") != null) order.setTaxNo(str(body.get("taxNo")));
        if (body.get("warehouseId") != null) order.setWarehouseId(toLong(body.get("warehouseId")));
        if (body.get("warehouseName") != null) order.setWarehouseName(str(body.get("warehouseName")));
        if (body.get("handlerId") != null) order.setHandlerId(toLong(body.get("handlerId")));
        if (body.get("handlerName") != null) order.setHandlerName(str(body.get("handlerName")));
        if (body.get("deptId") != null) order.setDeptId(toLong(body.get("deptId")));
        if (body.get("deptName") != null) order.setDeptName(str(body.get("deptName")));
        order.setOrderDate(toLocalDate(body.get("orderDate")));
        if (body.get("saleType") != null) order.setSaleType(toInt(body.get("saleType")));
        if (body.get("receiverName") != null) order.setReceiverName(str(body.get("receiverName")));
        if (body.get("receiverPhone") != null) order.setReceiverPhone(str(body.get("receiverPhone")));
        if (body.get("shippingAddress") != null) order.setShippingAddress(str(body.get("shippingAddress")));
        if (body.get("customerLevel") != null) order.setCustomerLevel(str(body.get("customerLevel")));
        if (body.get("customerTicket") != null) order.setCustomerTicket(str(body.get("customerTicket")));
        if (body.get("customerRemark") != null) order.setCustomerRemark(str(body.get("customerRemark")));
        if (body.get("status") != null) order.setStatus(toInt(body.get("status")));
        if (body.get("totalAmount") != null) order.setTotalAmount(toBigDecimal(body.get("totalAmount")));
        if (body.get("discountedAmount") != null) order.setDiscountedAmount(toBigDecimal(body.get("discountedAmount")));
        if (body.get("orderAmount") != null) order.setOrderAmount(toBigDecimal(body.get("orderAmount")));
        if (body.get("settlementStatus") != null) order.setSettlementStatus(toInt(body.get("settlementStatus")));
        if (body.get("receivedDeposit") != null) order.setReceivedDeposit(toBigDecimal(body.get("receivedDeposit")));
        if (body.get("unreceivedDeposit") != null) order.setUnreceivedDeposit(toBigDecimal(body.get("unreceivedDeposit")));
        if (body.get("depositBalance") != null) order.setDepositBalance(toBigDecimal(body.get("depositBalance")));
        if (body.get("depositAccount1") != null) order.setDepositAccount1(str(body.get("depositAccount1")));
        if (body.get("depositAccount2") != null) order.setDepositAccount2(str(body.get("depositAccount2")));
        if (body.get("depositAccount3") != null) order.setDepositAccount3(str(body.get("depositAccount3")));
        if (body.get("depositAccount4") != null) order.setDepositAccount4(str(body.get("depositAccount4")));
        if (body.get("depositAmount") != null) order.setDepositAmount(toBigDecimal(body.get("depositAmount")));
        if (body.get("creditLimit") != null) order.setCreditLimit(toBigDecimal(body.get("creditLimit")));
        order.setDepositDeadline(toLocalDate(body.get("depositDeadline")));
        if (body.get("preOrderQuantity") != null) order.setPreOrderQuantity(toBigDecimal(body.get("preOrderQuantity")));
        if (body.get("orderedQuantity") != null) order.setOrderedQuantity(toBigDecimal(body.get("orderedQuantity")));
        if (body.get("shippedQuantity") != null) order.setShippedQuantity(toBigDecimal(body.get("shippedQuantity")));
        if (body.get("totalWeight") != null) order.setTotalWeight(toBigDecimal(body.get("totalWeight")));
        if (body.get("totalVolume") != null) order.setTotalVolume(toBigDecimal(body.get("totalVolume")));
        if (body.get("region") != null) order.setRegion(str(body.get("region")));
        if (body.get("summary") != null) order.setSummary(str(body.get("summary")));
        if (body.get("remark") != null) order.setRemark(str(body.get("remark")));
        if (body.get("attachment") != null) order.setAttachment(str(body.get("attachment")));
        if (body.get("footerExtText1") != null) order.setFooterExtText1(str(body.get("footerExtText1")));
        if (body.get("footerExtText2") != null) order.setFooterExtText2(str(body.get("footerExtText2")));
        if (body.get("extNum1") != null) order.setExtNum1(toBigDecimal(body.get("extNum1")));
        if (body.get("extNum2") != null) order.setExtNum2(toBigDecimal(body.get("extNum2")));
        if (body.get("extText1") != null) order.setExtText1(str(body.get("extText1")));
        if (body.get("extText2") != null) order.setExtText2(str(body.get("extText2")));
        if (body.get("extText3") != null) order.setExtText3(str(body.get("extText3")));
        return order;
    }

    @SuppressWarnings("unchecked")
    private List<SalePreOrderItem> buildItemsFromMap(Map<String, Object> body) {
        Object itemsObj = body.get("items");
        if (!(itemsObj instanceof List)) return Collections.emptyList();

        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) itemsObj;
        List<SalePreOrderItem> items = new ArrayList<>();

        for (Map<String, Object> itemMap : itemsList) {
            SalePreOrderItem item = new SalePreOrderItem();
            // tenant_id 由 MyBatis-Plus MetaObjectHandler 自动填充

            if (itemMap.get("productId") != null) item.setProductId(toLong(itemMap.get("productId")));
            if (itemMap.get("productName") != null) item.setProductName(str(itemMap.get("productName")));
            if (itemMap.get("productCode") != null) item.setProductCode(str(itemMap.get("productCode")));
            if (itemMap.get("barcode") != null) item.setBarcode(str(itemMap.get("barcode")));
            if (itemMap.get("imageUrl") != null) item.setImageUrl(str(itemMap.get("imageUrl")));
            if (itemMap.get("specification") != null) item.setSpecification(str(itemMap.get("specification")));
            if (itemMap.get("model") != null) item.setModel(str(itemMap.get("model")));
            if (itemMap.get("origin") != null) item.setOrigin(str(itemMap.get("origin")));
            if (itemMap.get("brand") != null) item.setBrand(str(itemMap.get("brand")));
            if (itemMap.get("unit") != null) item.setUnit(str(itemMap.get("unit")));
            if (itemMap.get("pricingUnit") != null) item.setPricingUnit(str(itemMap.get("pricingUnit")));
            if (itemMap.get("smallUnit") != null) item.setSmallUnit(str(itemMap.get("smallUnit")));
            if (itemMap.get("smallUnitQuantity") != null) item.setSmallUnitQuantity(toBigDecimal(itemMap.get("smallUnitQuantity")));
            if (itemMap.get("conversionRelation") != null) item.setConversionRelation(str(itemMap.get("conversionRelation")));
            if (itemMap.get("conversionResult") != null) item.setConversionResult(toBigDecimal(itemMap.get("conversionResult")));
            if (itemMap.get("quantity") != null) item.setQuantity(toBigDecimal(itemMap.get("quantity")));
            if (itemMap.get("pieceQuantity") != null) item.setPieceQuantity(toBigDecimal(itemMap.get("pieceQuantity")));
            if (itemMap.get("bigPack") != null) item.setBigPack(toBigDecimal(itemMap.get("bigPack")));
            if (itemMap.get("midPack") != null) item.setMidPack(toBigDecimal(itemMap.get("midPack")));
            if (itemMap.get("smallPack") != null) item.setSmallPack(toBigDecimal(itemMap.get("smallPack")));
            if (itemMap.get("unitPrice") != null) item.setUnitPrice(toBigDecimal(itemMap.get("unitPrice")));
            if (itemMap.get("amount") != null) item.setAmount(toBigDecimal(itemMap.get("amount")));
            if (itemMap.get("smallUnitPrice") != null) item.setSmallUnitPrice(toBigDecimal(itemMap.get("smallUnitPrice")));
            if (itemMap.get("discountRate") != null) item.setDiscountRate(toBigDecimal(itemMap.get("discountRate")));
            if (itemMap.get("discountedPrice") != null) item.setDiscountedPrice(toBigDecimal(itemMap.get("discountedPrice")));
            if (itemMap.get("discountedAmount") != null) item.setDiscountedAmount(toBigDecimal(itemMap.get("discountedAmount")));
            if (itemMap.get("costPrice") != null) item.setCostPrice(toBigDecimal(itemMap.get("costPrice")));
            if (itemMap.get("costAmount") != null) item.setCostAmount(toBigDecimal(itemMap.get("costAmount")));
            if (itemMap.get("grossProfit") != null) item.setGrossProfit(toBigDecimal(itemMap.get("grossProfit")));
            if (itemMap.get("retailPrice") != null) item.setRetailPrice(toBigDecimal(itemMap.get("retailPrice")));
            if (itemMap.get("wholesalePrice") != null) item.setWholesalePrice(toBigDecimal(itemMap.get("wholesalePrice")));
            if (itemMap.get("minSalePrice") != null) item.setMinSalePrice(toBigDecimal(itemMap.get("minSalePrice")));
            item.setLastSaleDate(toLocalDate(itemMap.get("lastSaleDate")));
            if (itemMap.get("volume") != null) item.setVolume(toBigDecimal(itemMap.get("volume")));
            if (itemMap.get("weight") != null) item.setWeight(toBigDecimal(itemMap.get("weight")));
            if (itemMap.get("availableStock") != null) item.setAvailableStock(toBigDecimal(itemMap.get("availableStock")));
            if (itemMap.get("availableStockConversion") != null) item.setAvailableStockConversion(toBigDecimal(itemMap.get("availableStockConversion")));
            if (itemMap.get("bookStock") != null) item.setBookStock(toBigDecimal(itemMap.get("bookStock")));
            if (itemMap.get("region") != null) item.setRegion(str(itemMap.get("region")));
            if (itemMap.get("location") != null) item.setLocation(str(itemMap.get("location")));
            if (itemMap.get("productAttribute") != null) item.setProductAttribute(str(itemMap.get("productAttribute")));
            if (itemMap.get("gift") != null) item.setGift(Boolean.TRUE.equals(itemMap.get("gift")) || Integer.valueOf(1).equals(itemMap.get("gift")));
            if (itemMap.get("remark") != null) item.setRemark(str(itemMap.get("remark")));

            // 价格等级
            for (int i = 1; i <= 8; i++) {
                String key = "priceLevel" + i;
                if (itemMap.get(key) != null) {
                    switch (i) {
                        case 1: item.setPriceLevel1(toBigDecimal(itemMap.get(key))); break;
                        case 2: item.setPriceLevel2(toBigDecimal(itemMap.get(key))); break;
                        case 3: item.setPriceLevel3(toBigDecimal(itemMap.get(key))); break;
                        case 4: item.setPriceLevel4(toBigDecimal(itemMap.get(key))); break;
                        case 5: item.setPriceLevel5(toBigDecimal(itemMap.get(key))); break;
                        case 6: item.setPriceLevel6(toBigDecimal(itemMap.get(key))); break;
                        case 7: item.setPriceLevel7(toBigDecimal(itemMap.get(key))); break;
                        case 8: item.setPriceLevel8(toBigDecimal(itemMap.get(key))); break;
                    }
                }
            }

            // 自定义字段
            if (itemMap.get("extNum1") != null) item.setExtNum1(toBigDecimal(itemMap.get("extNum1")));
            if (itemMap.get("extNum2") != null) item.setExtNum2(toBigDecimal(itemMap.get("extNum2")));
            if (itemMap.get("extNum3") != null) item.setExtNum3(toBigDecimal(itemMap.get("extNum3")));
            if (itemMap.get("extNum4") != null) item.setExtNum4(toBigDecimal(itemMap.get("extNum4")));
            if (itemMap.get("extNum5") != null) item.setExtNum5(toBigDecimal(itemMap.get("extNum5")));
            if (itemMap.get("extText1") != null) item.setExtText1(str(itemMap.get("extText1")));
            if (itemMap.get("extText2") != null) item.setExtText2(str(itemMap.get("extText2")));
            if (itemMap.get("extPartner") != null) item.setExtPartner(toLong(itemMap.get("extPartner")));
            if (itemMap.get("extStaff") != null) item.setExtStaff(toLong(itemMap.get("extStaff")));
            if (itemMap.get("extDept") != null) item.setExtDept(toLong(itemMap.get("extDept")));

            items.add(item);
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private void saveItems(Long orderId, Map<String, Object> body) {
        Object itemsObj = body.get("items");
        if (!(itemsObj instanceof List)) return;

        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) itemsObj;
        int lineNo = 1;
        for (Map<String, Object> itemMap : itemsList) {
            SalePreOrderItem item = new SalePreOrderItem();
            item.setOrderId(orderId);
            item.setLineNo(lineNo++);
            if (itemMap.get("productId") != null) item.setProductId(toLong(itemMap.get("productId")));
            if (itemMap.get("productName") != null) item.setProductName(str(itemMap.get("productName")));
            if (itemMap.get("productCode") != null) item.setProductCode(str(itemMap.get("productCode")));
            if (itemMap.get("barcode") != null) item.setBarcode(str(itemMap.get("barcode")));
            if (itemMap.get("specification") != null) item.setSpecification(str(itemMap.get("specification")));
            if (itemMap.get("model") != null) item.setModel(str(itemMap.get("model")));
            if (itemMap.get("origin") != null) item.setOrigin(str(itemMap.get("origin")));
            if (itemMap.get("brand") != null) item.setBrand(str(itemMap.get("brand")));
            if (itemMap.get("unit") != null) item.setUnit(str(itemMap.get("unit")));
            if (itemMap.get("pricingUnit") != null) item.setPricingUnit(str(itemMap.get("pricingUnit")));
            if (itemMap.get("smallUnit") != null) item.setSmallUnit(str(itemMap.get("smallUnit")));
            if (itemMap.get("conversionRelation") != null) item.setConversionRelation(str(itemMap.get("conversionRelation")));
            if (itemMap.get("quantity") != null) item.setQuantity(toBigDecimal(itemMap.get("quantity")));
            if (itemMap.get("pieceQuantity") != null) item.setPieceQuantity(toBigDecimal(itemMap.get("pieceQuantity")));
            if (itemMap.get("bigPack") != null) item.setBigPack(toBigDecimal(itemMap.get("bigPack")));
            if (itemMap.get("midPack") != null) item.setMidPack(toBigDecimal(itemMap.get("midPack")));
            if (itemMap.get("smallPack") != null) item.setSmallPack(toBigDecimal(itemMap.get("smallPack")));
            if (itemMap.get("unitPrice") != null) item.setUnitPrice(toBigDecimal(itemMap.get("unitPrice")));
            if (itemMap.get("amount") != null) item.setAmount(toBigDecimal(itemMap.get("amount")));
            if (itemMap.get("smallUnitPrice") != null) item.setSmallUnitPrice(toBigDecimal(itemMap.get("smallUnitPrice")));
            if (itemMap.get("smallUnitQuantity") != null) item.setSmallUnitQuantity(toBigDecimal(itemMap.get("smallUnitQuantity")));
            if (itemMap.get("discountRate") != null) item.setDiscountRate(toBigDecimal(itemMap.get("discountRate")));
            if (itemMap.get("discountedPrice") != null) item.setDiscountedPrice(toBigDecimal(itemMap.get("discountedPrice")));
            if (itemMap.get("discountedAmount") != null) item.setDiscountedAmount(toBigDecimal(itemMap.get("discountedAmount")));
            if (itemMap.get("costPrice") != null) item.setCostPrice(toBigDecimal(itemMap.get("costPrice")));
            if (itemMap.get("costAmount") != null) item.setCostAmount(toBigDecimal(itemMap.get("costAmount")));
            if (itemMap.get("grossProfit") != null) item.setGrossProfit(toBigDecimal(itemMap.get("grossProfit")));
            if (itemMap.get("volume") != null) item.setVolume(toBigDecimal(itemMap.get("volume")));
            if (itemMap.get("weight") != null) item.setWeight(toBigDecimal(itemMap.get("weight")));
            if (itemMap.get("availableStock") != null) item.setAvailableStock(toBigDecimal(itemMap.get("availableStock")));
            if (itemMap.get("bookStock") != null) item.setBookStock(toBigDecimal(itemMap.get("bookStock")));
            if (itemMap.get("region") != null) item.setRegion(str(itemMap.get("region")));
            if (itemMap.get("location") != null) item.setLocation(str(itemMap.get("location")));
            if (itemMap.get("productAttribute") != null) item.setProductAttribute(str(itemMap.get("productAttribute")));
            if (itemMap.get("gift") != null) item.setGift(Boolean.TRUE.equals(itemMap.get("gift")) || Integer.valueOf(1).equals(itemMap.get("gift")));
            if (itemMap.get("remark") != null) item.setRemark(str(itemMap.get("remark")));
            itemMapper.insert(item);
        }
    }

    private String str(Object val) {
        return val != null ? val.toString() : null;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return new BigDecimal(val.toString());
    }

    /** 日期解析容错：空串/空白（前端未选日期时传 ''）返回 null，避免 DateTimeParseException */
    private LocalDate toLocalDate(Object val) {
        String s = str(val);
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim().length() > 10 ? s.trim().substring(0, 10) : s.trim());
        } catch (Exception e) {
            log.warn("日期格式非法，已忽略: {}", s);
            return null;
        }
    }
}
