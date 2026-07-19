package cn.aiedge.erp.sale.outbound.controller;

import cn.aiedge.erp.sale.outbound.dto.SaleOutboundCreateDTO;
import cn.aiedge.erp.sale.outbound.dto.SaleOutboundItemDTO;
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

@Slf4j
@RestController
@RequestMapping("/api/erp/sale/outbound")
@RequiredArgsConstructor
@Tag(name = "销售出库管理", description = "销售出库单创建、审批、拣货、打包、发货等操作")
public class SaleOutboundController {

    private final SaleOutboundService saleOutboundService;
    private final SaleOutboundMapper saleOutboundMapper;

    @GetMapping("/page")
    @Operation(summary = "分页查询出库单（按单据）")
    public Page<SaleOutboundVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "出库单号") @RequestParam(required = false) String outboundNo,
            @Parameter(description = "经手人ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "结算状态") @RequestParam(required = false) String settlementStatus,
            @Parameter(description = "结款方式") @RequestParam(required = false) String settlementMethod,
            @Parameter(description = "来源订单") @RequestParam(required = false) String sourceOrder,
            @Parameter(description = "收货人") @RequestParam(required = false) String receiverName,
            @Parameter(description = "开始日期") @RequestParam(required = false) String dateStart,
            @Parameter(description = "结束日期") @RequestParam(required = false) String dateEnd,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<SaleOutbound> page = saleOutboundService.pageList(keyword, customerId, orderId, warehouseId, status,
                outboundNo, salesPersonId, settlementStatus, settlementMethod, sourceOrder, receiverName,
                dateStart, dateEnd, pageNum, pageSize);
        Page<SaleOutboundVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/page-detail")
    @Operation(summary = "分页查询出库单明细（按明细）")
    public Page<Map<String, Object>> pageDetail(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "出库单号") @RequestParam(required = false) String outboundNo,
            @Parameter(description = "商品ID") @RequestParam(required = false) Long productId,
            @Parameter(description = "经手人ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "结算状态") @RequestParam(required = false) String settlementStatus,
            @Parameter(description = "来源订单") @RequestParam(required = false) String sourceOrder,
            @Parameter(description = "开始日期") @RequestParam(required = false) String dateStart,
            @Parameter(description = "结束日期") @RequestParam(required = false) String dateEnd,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return saleOutboundService.pageDetail(keyword, customerId, warehouseId, status, outboundNo,
                productId, salesPersonId, settlementStatus, sourceOrder, dateStart, dateEnd, pageNum, pageSize);
    }

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

    @GetMapping("/{id}/items")
    @Operation(summary = "获取出库明细")
    public List<SaleOutboundItem> getItems(@PathVariable Long id) {
        return saleOutboundService.getItems(id);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户的出库单列表")
    public List<SaleOutboundVO> listByCustomerId(@PathVariable Long customerId) {
        return saleOutboundService.listByCustomerId(customerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单的出库单列表")
    public List<SaleOutboundVO> listByOrderId(@PathVariable Long orderId) {
        return saleOutboundService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "创建出库单")
    public SaleOutboundVO create(@RequestBody SaleOutboundCreateDTO dto) {
        SaleOutbound outbound = new SaleOutbound();
        BeanUtils.copyProperties(dto, outbound);
        outbound.setTenantId(1L);
        outbound.setCreateBy(StpUtil.getLoginIdAsLong());
        List<SaleOutboundItem> items = mapItemsFromDTO(dto.getItems());
        SaleOutbound created = saleOutboundService.createOutbound(outbound, items);
        return convertToVO(created);
    }

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从销售订单创建出库单")
    public SaleOutboundVO createFromOrder(@PathVariable Long orderId) {
        SaleOutbound outbound = saleOutboundService.createFromOrder(orderId);
        return convertToVO(outbound);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新出库单")
    public SaleOutboundVO update(@PathVariable Long id, @RequestBody SaleOutboundCreateDTO dto) {
        SaleOutbound outbound = new SaleOutbound();
        BeanUtils.copyProperties(dto, outbound);
        List<SaleOutboundItem> items = mapItemsFromDTO(dto.getItems());
        SaleOutbound updated = saleOutboundService.updateOutbound(id, outbound, items);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public SaleOutboundVO submitForApproval(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.submitForApproval(id);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public SaleOutboundVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        SaleOutbound outbound = saleOutboundService.approve(id, approverId, note);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public SaleOutboundVO reject(@PathVariable Long id, @RequestParam String reason) {
        SaleOutbound outbound = saleOutboundService.reject(id, reason);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/start-picking")
    @Operation(summary = "开始拣货")
    public SaleOutboundVO startPicking(@PathVariable Long id) {
        Long pickerId = StpUtil.getLoginIdAsLong();
        SaleOutbound outbound = saleOutboundService.startPicking(id, pickerId);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/items/{itemId}/pick")
    @Operation(summary = "拣货明细处理")
    public SaleOutboundItem pickItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal outboundQuantity,
            @RequestParam(required = false) String batchNo) {
        return saleOutboundService.pickItem(itemId, outboundQuantity, batchNo);
    }

    @PostMapping("/{id}/complete-picking")
    @Operation(summary = "完成拣货")
    public SaleOutboundVO completePicking(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.completePicking(id);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/start-packing")
    @Operation(summary = "开始打包")
    public SaleOutboundVO startPacking(@PathVariable Long id) {
        Long packerId = StpUtil.getLoginIdAsLong();
        SaleOutbound outbound = saleOutboundService.startPacking(id, packerId);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/items/{itemId}/pack")
    @Operation(summary = "打包明细处理")
    public SaleOutboundItem packItem(@PathVariable Long itemId) {
        return saleOutboundService.packItem(itemId);
    }

    @PostMapping("/{id}/complete-packing")
    @Operation(summary = "完成打包")
    public SaleOutboundVO completePacking(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.completePacking(id);
        return convertToVO(outbound);
    }

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

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成出库")
    public SaleOutboundVO complete(@PathVariable Long id) {
        SaleOutbound outbound = saleOutboundService.complete(id);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消出库")
    public SaleOutboundVO cancel(@PathVariable Long id, @RequestParam String reason) {
        SaleOutbound outbound = saleOutboundService.cancel(id, reason);
        return convertToVO(outbound);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加出库明细")
    public SaleOutboundItem addItem(@PathVariable Long id, @RequestBody SaleOutboundItemDTO dto) {
        SaleOutboundItem item = new SaleOutboundItem();
        BeanUtils.copyProperties(dto, item);
        return saleOutboundService.addItem(id, item);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新出库明细")
    public SaleOutboundItem updateItem(@PathVariable Long itemId, @RequestBody SaleOutboundItemDTO dto) {
        SaleOutboundItem item = new SaleOutboundItem();
        BeanUtils.copyProperties(dto, item);
        return saleOutboundService.updateItem(itemId, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除出库明细")
    public void removeItem(@PathVariable Long itemId) {
        saleOutboundService.removeItem(itemId);
    }

    @GetMapping("/calculate-price")
    @Operation(summary = "计算商品价格（前端选品时调用）")
    public Map<String, Object> calculatePrice(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) BigDecimal unitPrice) {
        return saleOutboundService.calculateItemPrice(customerId, productId, quantity, unitPrice);
    }

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

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除出库单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return saleOutboundService.removeBatchByIds(ids);
    }

    @GetMapping("/export")
    @Operation(summary = "导出售库单列表")
    public List<SaleOutbound> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return saleOutboundService.exportList(keyword, status);
    }

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