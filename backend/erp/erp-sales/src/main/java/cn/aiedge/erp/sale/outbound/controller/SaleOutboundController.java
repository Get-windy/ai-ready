package cn.aiedge.erp.sale.outbound.controller;

import cn.aiedge.erp.sale.outbound.dto.SaleOutboundCreateDTO;
import cn.aiedge.erp.sale.outbound.dto.SaleOutboundItemDTO;
import cn.aiedge.erp.sale.outbound.dto.SaleOutboundQueryDTO;
import cn.aiedge.erp.sale.outbound.dto.SaleOutboundVO;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.enums.OutboundStatus;
import cn.aiedge.erp.sale.outbound.mapper.SaleOutboundMapper;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/sale/outbound")
@RequiredArgsConstructor
@Tag(name = "销售出库管理", description = "销售出库单创建、审批、拣货、打包、发货等操作")
public class SaleOutboundController {

    private final SaleOutboundService saleOutboundService;
    private final SaleOutboundMapper saleOutboundMapper;

    @SaCheckPermission("sale:outbound:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询出库单（按单据，对标文档 40 项查询条件）")
    public Page<SaleOutboundVO> page(SaleOutboundQueryDTO query) {
        Page<SaleOutbound> page = saleOutboundService.pageList(query);
        Page<SaleOutboundVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("sale:outbound:view")
    @GetMapping("/page-detail")
    @Operation(summary = "分页查询出库单明细（按明细，分页口径 = 明细行）")
    public Page<Map<String, Object>> pageDetail(SaleOutboundQueryDTO query) {
        return saleOutboundService.pageDetail(query);
    }

    @SaCheckPermission("sale:outbound:list")
    @GetMapping("/next-no")
    @Operation(summary = "获取下一个出库单号（后端号段，前端禁止自增演示号）")
    public String nextNo() {
        // 与采购/入库等模块一致：直接返回完整单号字符串，前端 generateCodeAsync 原样使用
        return saleOutboundService.generateOutboundNo();
    }

    @SaCheckPermission("sale:outbound:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取出库单详情")
    public SaleOutboundVO getById(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.getById(id);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        SaleOutboundVO vo = convertToVO(outbound);
        vo.setItems(saleOutboundService.getItems(id));
        return vo;
    }

    @SaCheckPermission("sale:outbound:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取出库明细")
    public List<SaleOutboundItem> getItems(@PathVariable Long id) {
        return saleOutboundService.getItems(id);
    }

    @SaCheckPermission("sale:outbound:detail")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户的出库单列表")
    public List<SaleOutboundVO> listByCustomerId(@PathVariable Long customerId) {
        return saleOutboundService.listByCustomerId(customerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("sale:outbound:detail")
    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单的出库单列表")
    public List<SaleOutboundVO> listByOrderId(@PathVariable Long orderId) {
        return saleOutboundService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("sale:outbound:create")
    @PostMapping
    @Operation(summary = "创建出库单")
    public SaleOutboundVO create(@RequestBody SaleOutboundCreateDTO dto) {
        SaleOutbound outbound = new SaleOutbound();
        BeanUtils.copyProperties(dto, outbound);
        // 来源订单：前端「源单」输入框字段名为 sourceOrder，实体字段为 orderNo
        if ((outbound.getOrderNo() == null || outbound.getOrderNo().isBlank())
                && dto.getSourceOrder() != null && !dto.getSourceOrder().isBlank()) {
            outbound.setOrderNo(dto.getSourceOrder());
        }
        outbound.setTenantId(1L);
        outbound.setCreateBy(StpUtil.getLoginIdAsLong());
        List<SaleOutboundItem> items = mapItemsFromDTO(dto.getItems());
        SaleOutbound created = saleOutboundService.createOutbound(outbound, items);
        return convertToVO(created);
    }

    @SaCheckPermission("sale:outbound:create")
    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从销售订单创建出库单")
    public SaleOutboundVO createFromOrder(@PathVariable Long orderId) {
        SaleOutbound outbound = saleOutboundService.createFromOrder(orderId);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新出库单")
    public SaleOutboundVO update(@PathVariable Long id, @RequestBody SaleOutboundCreateDTO dto) {
        SaleOutbound outbound = new SaleOutbound();
        BeanUtils.copyProperties(dto, outbound);
        if ((outbound.getOrderNo() == null || outbound.getOrderNo().isBlank())
                && dto.getSourceOrder() != null && !dto.getSourceOrder().isBlank()) {
            outbound.setOrderNo(dto.getSourceOrder());
        }
        List<SaleOutboundItem> items = mapItemsFromDTO(dto.getItems());
        SaleOutbound updated = saleOutboundService.updateOutbound(id, outbound, items);
        return convertToVO(updated);
    }

    @SaCheckPermission("sale:outbound:submit")
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public SaleOutboundVO submitForApproval(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.submitForApproval(id);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:approve")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public SaleOutboundVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        SaleOutbound outbound = saleOutboundService.approve(id, approverId, note);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:approve")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public SaleOutboundVO reject(@PathVariable Long id, @RequestParam String reason) {
        SaleOutbound outbound = saleOutboundService.reject(id, reason);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/start-picking")
    @Operation(summary = "开始拣货")
    public SaleOutboundVO startPicking(@PathVariable Long id) {
        Long pickerId = StpUtil.getLoginIdAsLong();
        SaleOutbound outbound = saleOutboundService.startPicking(id, pickerId);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/items/{itemId}/pick")
    @Operation(summary = "拣货明细处理")
    public SaleOutboundItem pickItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal outboundQuantity,
            @RequestParam(required = false) String batchNo) {
        return saleOutboundService.pickItem(itemId, outboundQuantity, batchNo);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/complete-picking")
    @Operation(summary = "完成拣货")
    public SaleOutboundVO completePicking(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.completePicking(id);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/start-packing")
    @Operation(summary = "开始打包")
    public SaleOutboundVO startPacking(@PathVariable Long id) {
        Long packerId = StpUtil.getLoginIdAsLong();
        SaleOutbound outbound = saleOutboundService.startPacking(id, packerId);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/items/{itemId}/pack")
    @Operation(summary = "打包明细处理")
    public SaleOutboundItem packItem(@PathVariable Long itemId) {
        return saleOutboundService.packItem(itemId);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/complete-packing")
    @Operation(summary = "完成打包")
    public SaleOutboundVO completePacking(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.completePacking(id);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/ship")
    @Operation(summary = "发货")
    public SaleOutboundVO ship(
            @PathVariable Long id,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) String logisticsCompany) {
        Long shipperId = StpUtil.getLoginIdAsLong();
        SaleOutbound outbound = saleOutboundService.ship(id, shipperId, trackingNumber, logisticsCompany);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:complete")
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成出库")
    public SaleOutboundVO complete(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.complete(id);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:cancel")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消出库")
    public SaleOutboundVO cancel(@PathVariable Long id, @RequestParam String reason) {
        SaleOutbound outbound = saleOutboundService.cancel(id, reason);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/{id}/items")
    @Operation(summary = "添加出库明细")
    public SaleOutboundItem addItem(@PathVariable Long id, @RequestBody SaleOutboundItemDTO dto) {
        SaleOutboundItem item = new SaleOutboundItem();
        BeanUtils.copyProperties(dto, item);
        return saleOutboundService.addItem(id, item);
    }

    @SaCheckPermission("sale:outbound:update")
    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新出库明细")
    public SaleOutboundItem updateItem(@PathVariable Long itemId, @RequestBody SaleOutboundItemDTO dto) {
        SaleOutboundItem item = new SaleOutboundItem();
        BeanUtils.copyProperties(dto, item);
        return saleOutboundService.updateItem(itemId, item);
    }

    @SaCheckPermission("sale:outbound:delete")
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除出库明细")
    public void removeItem(@PathVariable Long itemId) {
        saleOutboundService.removeItem(itemId);
    }

    @SaCheckPermission("sale:outbound:view")
    @GetMapping("/calculate-price")
    @Operation(summary = "计算商品价格（前端选品时调用）")
    public Map<String, Object> calculatePrice(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) BigDecimal unitPrice) {
        return saleOutboundService.calculateItemPrice(customerId, productId, quantity, unitPrice);
    }

    @SaCheckPermission("sale:outbound:view")
    @GetMapping("/statistics")
    @Operation(summary = "出库统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (OutboundStatus status : OutboundStatus.values()) {
            stats.put(status.getDesc(), saleOutboundService.lambdaQuery()
                    .eq(SaleOutbound::getStatus, status.getCode())
                    .eq(SaleOutbound::getDeleted, 0)
                    .count());
        }
        stats.put("totalOutboundAmount", saleOutboundMapper.sumOutboundAmount(1L));
        return stats;
    }

    @SaCheckPermission("sale:outbound:delete")
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除出库单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return saleOutboundService.removeBatchByIds(ids);
    }

    @SaCheckPermission("sale:outbound:export")
    @GetMapping("/export")
    @Operation(summary = "导出售库单列表（真实 Excel 流式输出）")
    public void export(SaleOutboundQueryDTO query, jakarta.servlet.http.HttpServletResponse response) {
        List<SaleOutbound> list = saleOutboundService.exportList(query);
        String fileName = "销售出库单_" + java.time.LocalDate.now() + ".xlsx";
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));
            try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("销售出库单");
                // 列定义与《发货查询》页面 49 列一一对应（顺序/文案一致，避免"导出与页面口径不一致"）
                // 说明：客户一票通（仅明细有列）、附件（表头无列）本系统无表头数据源，导出留空；
                //      本单金额 = 商品金额 − 促销优惠 − 优惠劵 − 直接优惠 + 运费 + 其他费用（与页面同一口径）
                String[] headers = {"单据日期", "单据编号", "单据状态", "来源订单", "仓库", "客户", "客户编号",
                        "客户级别", "收货人", "联系电话", "收货地址", "物流公司", "运单号", "客户一票通", "客户备注",
                        "经手人", "部门", "商品金额", "促销优惠", "优惠劵", "直接优惠", "运费承担方", "运费",
                        "其他费用", "本单金额", "已结金额", "结算状态", "数量", "配送方式", "重量（kg）", "体积（m³）",
                        "产生方式", "单据备注", "摘要", "附件", "表头自定义字段1(数字)", "表头自定义字段2(数字)",
                        "表头自定义字段3(文本)", "表头自定义字段4(文本)", "表头自定义字段5(文本)",
                        "表尾自定义字段1(文本)", "表尾自定义字段2(文本)", "制单人", "记账人", "审核人",
                        "记账时间", "制单时间", "打印次数", "打印时间"};
                org.apache.poi.ss.usermodel.Row head = sheet.createRow(0);
                org.apache.poi.ss.usermodel.CellStyle headStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headFont = workbook.createFont();
                headFont.setBold(true);
                headStyle.setFont(headFont);
                for (int i = 0; i < headers.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = head.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headStyle);
                }
                int rowIdx = 1;
                for (SaleOutbound o : list) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                    int c = 0;
                    row.createCell(c++).setCellValue(o.getOutboundDate() == null ? "" : o.getOutboundDate().toString());
                    row.createCell(c++).setCellValue(nvl(o.getOutboundNo()));
                    row.createCell(c++).setCellValue(statusDesc(o.getStatus()));
                    row.createCell(c++).setCellValue(nvl(o.getOrderNo()));
                    row.createCell(c++).setCellValue(nvl(o.getWarehouseName()));
                    row.createCell(c++).setCellValue(nvl(o.getCustomerName()));
                    row.createCell(c++).setCellValue(nvl(o.getCustomerCode()));
                    row.createCell(c++).setCellValue(nvl(o.getCustomerLevel()));
                    row.createCell(c++).setCellValue(nvl(o.getReceiverName()));
                    row.createCell(c++).setCellValue(nvl(o.getReceiverPhone()));
                    row.createCell(c++).setCellValue(nvl(o.getShippingAddress()));
                    row.createCell(c++).setCellValue(nvl(o.getLogisticsCompany()));
                    row.createCell(c++).setCellValue(nvl(o.getTrackingNumber()));
                    row.createCell(c++).setCellValue("");   // 客户一票通：表头无数据源
                    row.createCell(c++).setCellValue(nvl(o.getCustomerRemark()));
                    row.createCell(c++).setCellValue(nvl(o.getSalesPersonName()));
                    row.createCell(c++).setCellValue(nvl(o.getDepartmentName()));
                    row.createCell(c++).setCellValue(num(o.getTotalAmount()));
                    row.createCell(c++).setCellValue(num(o.getPromoDiscount()));
                    row.createCell(c++).setCellValue(num(o.getCouponAmount()));
                    row.createCell(c++).setCellValue(num(o.getDirectDiscount()));
                    row.createCell(c++).setCellValue(nvl(o.getFreightPayer()));
                    row.createCell(c++).setCellValue(num(o.getFreight()));
                    row.createCell(c++).setCellValue(num(o.getOtherFee()));
                    row.createCell(c++).setCellValue(billAmount(o).doubleValue());
                    row.createCell(c++).setCellValue(num(o.getSettledAmount()));
                    row.createCell(c++).setCellValue(nvl(o.getSettlementStatus()));
                    row.createCell(c++).setCellValue(num(o.getTotalQuantity()));
                    row.createCell(c++).setCellValue(nvl(o.getDeliveryMethod()));
                    row.createCell(c++).setCellValue(num(o.getTotalWeight()));
                    row.createCell(c++).setCellValue(num(o.getTotalVolume()));
                    row.createCell(c++).setCellValue(nvl(o.getGenerationMethod()));
                    row.createCell(c++).setCellValue(nvl(o.getRemark()));
                    row.createCell(c++).setCellValue(nvl(o.getSummary()));
                    row.createCell(c++).setCellValue("");   // 附件：表头无列
                    row.createCell(c++).setCellValue(num(o.getExtNum1()));
                    row.createCell(c++).setCellValue(num(o.getExtNum2()));
                    row.createCell(c++).setCellValue(nvl(o.getExtText1()));
                    row.createCell(c++).setCellValue(nvl(o.getExtText2()));
                    row.createCell(c++).setCellValue(nvl(o.getExtText3()));
                    row.createCell(c++).setCellValue(nvl(o.getFooterExtText1()));
                    row.createCell(c++).setCellValue(nvl(o.getFooterExtText2()));
                    row.createCell(c++).setCellValue(nvl(o.getCreatorName()));
                    row.createCell(c++).setCellValue(nvl(o.getBookkeeperName()));
                    row.createCell(c++).setCellValue(nvl(o.getAuditorName()));
                    row.createCell(c++).setCellValue(o.getBookkeepingTime() == null ? "" : o.getBookkeepingTime().toString());
                    row.createCell(c++).setCellValue(o.getCreateTime() == null ? "" : o.getCreateTime().toString());
                    row.createCell(c++).setCellValue(o.getPrintCount() == null ? 0 : o.getPrintCount());
                    row.createCell(c).setCellValue(o.getPrintTime() == null ? "" : o.getPrintTime().toString());
                }
                for (int i = 0; i < headers.length; i++) {
                    sheet.setColumnWidth(i, 16 * 256);
                }
                workbook.write(response.getOutputStream());
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            log.error("销售出库单导出失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @SaCheckPermission("sale:outbound:update")
    @PostMapping("/batch-logistics-remark")
    @Operation(summary = "批量写入物流备注（选中单据）")
    public Map<String, Object> batchLogisticsRemark(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("ids");
        if (!(idsObj instanceof List<?> rawIds) || rawIds.isEmpty()) {
            throw new IllegalArgumentException("请先选择出库单");
        }
        List<Long> ids = rawIds.stream()
                .map(v -> v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v)))
                .collect(Collectors.toList());
        String logisticsRemark = body.get("logisticsRemark") == null ? "" : String.valueOf(body.get("logisticsRemark"));
        int updated = saleOutboundService.batchUpdateLogisticsRemark(ids, logisticsRemark);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("updated", updated);
        return result;
    }

    private String nvl(String v) {
        return v == null ? "" : v;
    }

    private double num(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }

    /** 本单金额 = 商品金额 − 促销优惠 − 优惠劵 − 直接优惠 + 运费 + 其他费用（与《发货查询》页面同一口径） */
    private BigDecimal billAmount(SaleOutbound o) {
        return nz(o.getTotalAmount())
                .subtract(nz(o.getPromoDiscount()))
                .subtract(nz(o.getCouponAmount()))
                .subtract(nz(o.getDirectDiscount()))
                .add(nz(o.getFreight()))
                .add(nz(o.getOtherFee()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String statusDesc(Integer status) {
        if (status == null) {
            return "";
        }
        for (OutboundStatus s : OutboundStatus.values()) {
            if (s.getCode().equals(status)) {
                return s.getDesc();
            }
        }
        return "";
    }

    @SaCheckPermission("sale:outbound:print")
    @PostMapping("/batch-print")
    @Operation(summary = "批量打印出库单")
    public List<SaleOutboundVO> batchPrint(@RequestBody List<Long> ids) {
        return ids.stream().map(id -> {
            SaleOutbound outbound = saleOutboundService.getById(id);
            if (outbound != null) {
                outbound.setPrintCount(outbound.getPrintCount() != null ? outbound.getPrintCount() + 1 : 1);
                outbound.setPrintTime(java.time.LocalDateTime.now());
                saleOutboundService.updateById(outbound);
                return convertToVO(outbound);
            }
            return null;
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @SaCheckPermission("sale:outbound:print")
    @PostMapping("/{id}/print")
    @Operation(summary = "打印后更新打印次数")
    public SaleOutboundVO print(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.getById(id);
        if (outbound == null) {
            throw new RuntimeException("出库单不存在");
        }
        outbound.setPrintCount(outbound.getPrintCount() != null ? outbound.getPrintCount() + 1 : 1);
        outbound.setPrintTime(java.time.LocalDateTime.now());
        saleOutboundService.updateById(outbound);
        return convertToVO(outbound);
    }

    @SaCheckPermission("sale:outbound:create")
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制出库单")
    public SaleOutboundVO copy(@PathVariable Long id) {
        SaleOutbound source = saleOutboundService.getById(id);
        if (source == null) {
            throw new RuntimeException("出库单不存在");
        }
        SaleOutbound copied = saleOutboundService.copyOutbound(id);
        return convertToVO(copied);
    }

    @SaCheckPermission("sale:outbound:import")
    @PostMapping("/import")
    @Operation(summary = "批量导入出库单")
    public Map<String, Object> importOutbound(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = saleOutboundService.importOutbound(file);
            result.put("success", true);
            result.put("importedCount", count);
            result.put("message", "成功导入 " + count + " 条记录");
        } catch (Exception e) {
            log.error("批量导入失败", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return result;
    }

    private SaleOutboundVO convertToVO(SaleOutbound outbound) {
        SaleOutboundVO vo = new SaleOutboundVO();
        BeanUtils.copyProperties(outbound, vo);
        for (OutboundStatus status : OutboundStatus.values()) {
            if (status.getCode().equals(outbound.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }

    /** 将前端 DTO 列表映射为实体列表（处理字段名差异） */
    private List<SaleOutboundItem> mapItemsFromDTO(List<SaleOutboundItemDTO> itemDTOs) {
        if (itemDTOs == null) return null;
        return itemDTOs.stream().map(itemDTO -> {
            SaleOutboundItem item = new SaleOutboundItem();
            BeanUtils.copyProperties(itemDTO, item);
            // 前端字段名与实体字段名映射
            // specification: 前端传 specification, 实体有 specification 和 productSpec 两个兼容字段
            if (itemDTO.getSpecification() != null) {
                item.setSpecification(itemDTO.getSpecification());
                if (item.getProductSpec() == null) item.setProductSpec(itemDTO.getSpecification());
            }
            if (itemDTO.getProductSpec() != null && item.getSpecification() == null) {
                item.setSpecification(itemDTO.getProductSpec());
            }
            // quantity → outboundQuantity + orderQuantity
            if (itemDTO.getQuantity() != null) {
                item.setQuantity(itemDTO.getQuantity());
                item.setOutboundQuantity(itemDTO.getQuantity());
                item.setOrderQuantity(itemDTO.getQuantity());
            }
            // amount → lineAmount
            if (itemDTO.getAmount() != null) item.setLineAmount(itemDTO.getAmount());
            // 日期字符串 → LocalDate
            if (itemDTO.getExpiryDate() != null && item.getExpiryDate() == null) {
                try { item.setExpiryDate(LocalDate.parse(itemDTO.getExpiryDate())); } catch (Exception ignored) {}
            }
            if (itemDTO.getLastSaleDate() != null && item.getLastSaleDate() == null) {
                try { item.setLastSaleDate(LocalDate.parse(itemDTO.getLastSaleDate())); } catch (Exception ignored) {}
            }
            return item;
        }).collect(Collectors.toList());
    }
}