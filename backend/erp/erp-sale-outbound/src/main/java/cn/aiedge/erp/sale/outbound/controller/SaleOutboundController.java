package cn.aiedge.erp.sale.outbound.controller;

import cn.aiedge.erp.sale.outbound.dto.SaleOutboundCreateDTO;
import cn.aiedge.erp.sale.outbound.dto.SaleOutboundItemDTO;
import cn.aiedge.erp.sale.outbound.dto.SaleOutboundVO;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import cn.aiedge.erp.sale.outbound.enums.OutboundStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/erp/sale-outbound")
@RequiredArgsConstructor
@Tag(name = "销售出库管理", description = "销售出库单创建、审批、拣货、打包、发货等操作")
public class SaleOutboundController {

    private final SaleOutboundService saleOutboundService;

    @GetMapping("/page")
    @Operation(summary = "分页查询出库单")
    public Page<SaleOutboundVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<SaleOutbound> page = saleOutboundService.pageList(keyword, customerId, orderId, warehouseId, status, pageNum, pageSize);
        Page<SaleOutboundVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
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
        List<SaleOutboundItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                SaleOutboundItem item = new SaleOutboundItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
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
        List<SaleOutboundItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                SaleOutboundItem item = new SaleOutboundItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
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
        stats.put("totalOutboundAmount", saleOutboundService.baseMapper.sumOutboundAmount(1L));
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
}