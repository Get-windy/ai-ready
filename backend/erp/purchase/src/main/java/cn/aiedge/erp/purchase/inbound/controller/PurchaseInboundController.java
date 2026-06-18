package cn.aiedge.erp.purchase.inbound.controller;

import cn.aiedge.erp.purchase.inbound.dto.PurchaseInboundCreateDTO;
import cn.aiedge.erp.purchase.inbound.dto.PurchaseInboundItemDTO;
import cn.aiedge.erp.purchase.inbound.dto.PurchaseInboundVO;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem;
import cn.aiedge.erp.purchase.inbound.enums.InboundStatus;
import cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundMapper;
import cn.aiedge.erp.purchase.inbound.service.PurchaseInboundService;
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
@RequestMapping("/api/erp/purchase/inbound")
@RequiredArgsConstructor
@Tag(name = "采购入库管理", description = "采购入库单创建、审批、收货、质检、入库等操作")
public class PurchaseInboundController {

    private final PurchaseInboundService purchaseInboundService;
    private final PurchaseInboundMapper purchaseInboundMapper;

    @GetMapping("/page")
    @Operation(summary = "分页查询入库单")
    public Page<PurchaseInboundVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<PurchaseInbound> page = purchaseInboundService.pageList(keyword, supplierId, orderId, warehouseId, status, pageNum, pageSize);
        Page<PurchaseInboundVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取入库单详情")
    public PurchaseInboundVO getById(@PathVariable Long id) {
        PurchaseInbound inbound = purchaseInboundService.getById(id);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        PurchaseInboundVO vo = convertToVO(inbound);
        vo.setItems(purchaseInboundService.getItems(id));
        return vo;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取入库明细")
    public List<PurchaseInboundItem> getItems(@PathVariable Long id) {
        return purchaseInboundService.getItems(id);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "获取供应商的入库单列表")
    public List<PurchaseInboundVO> listBySupplierId(@PathVariable Long supplierId) {
        return purchaseInboundService.listBySupplierId(supplierId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单的入库单列表")
    public List<PurchaseInboundVO> listByOrderId(@PathVariable Long orderId) {
        return purchaseInboundService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "创建入库单")
    public PurchaseInboundVO create(@RequestBody PurchaseInboundCreateDTO dto) {
        PurchaseInbound inbound = new PurchaseInbound();
        BeanUtils.copyProperties(dto, inbound);
        inbound.setTenantId(1L);
        inbound.setCreateBy(StpUtil.getLoginIdAsLong());
        List<PurchaseInboundItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                PurchaseInboundItem item = new PurchaseInboundItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        PurchaseInbound created = purchaseInboundService.createInbound(inbound, items);
        return convertToVO(created);
    }

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从采购订单创建入库单")
    public PurchaseInboundVO createFromOrder(@PathVariable Long orderId) {
        PurchaseInbound inbound = purchaseInboundService.createFromOrder(orderId);
        return convertToVO(inbound);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新入库单")
    public PurchaseInboundVO update(@PathVariable Long id, @RequestBody PurchaseInboundCreateDTO dto) {
        PurchaseInbound inbound = new PurchaseInbound();
        BeanUtils.copyProperties(dto, inbound);
        List<PurchaseInboundItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                PurchaseInboundItem item = new PurchaseInboundItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        PurchaseInbound updated = purchaseInboundService.updateInbound(id, inbound, items);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public PurchaseInboundVO submitForApproval(@PathVariable Long id) {
        PurchaseInbound inbound = purchaseInboundService.submitForApproval(id);
        return convertToVO(inbound);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public PurchaseInboundVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        PurchaseInbound inbound = purchaseInboundService.approve(id, approverId, note);
        return convertToVO(inbound);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public PurchaseInboundVO reject(@PathVariable Long id, @RequestParam String reason) {
        PurchaseInbound inbound = purchaseInboundService.reject(id, reason);
        return convertToVO(inbound);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "收货")
    public PurchaseInboundVO receive(@PathVariable Long id) {
        Long receiverId = StpUtil.getLoginIdAsLong();
        PurchaseInbound inbound = purchaseInboundService.receive(id, receiverId);
        return convertToVO(inbound);
    }

    @PostMapping("/{id}/items/{itemId}/receive")
    @Operation(summary = "收货明细处理")
    public PurchaseInboundItem receiveItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal inboundQuantity,
            @RequestParam(required = false) String qualityNote) {
        return purchaseInboundService.receiveItem(itemId, inboundQuantity, qualityNote);
    }

    @PostMapping("/{id}/quality-check")
    @Operation(summary = "质检")
    public PurchaseInboundVO qualityCheck(@PathVariable Long id, @RequestParam String result) {
        Long checkerId = StpUtil.getLoginIdAsLong();
        PurchaseInbound inbound = purchaseInboundService.qualityCheck(id, checkerId, result);
        return convertToVO(inbound);
    }

    @PostMapping("/{id}/warehouse-confirm")
    @Operation(summary = "确认入库")
    public PurchaseInboundVO confirmWarehouse(@PathVariable Long id) {
        Long confirmerId = StpUtil.getLoginIdAsLong();
        PurchaseInbound inbound = purchaseInboundService.confirmWarehouse(id, confirmerId);
        return convertToVO(inbound);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成入库")
    public PurchaseInboundVO complete(@PathVariable Long id) {
        PurchaseInbound inbound = purchaseInboundService.complete(id);
        return convertToVO(inbound);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消入库")
    public PurchaseInboundVO cancel(@PathVariable Long id, @RequestParam String reason) {
        PurchaseInbound inbound = purchaseInboundService.cancel(id, reason);
        return convertToVO(inbound);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除入库单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return purchaseInboundService.removeBatchByIds(ids);
    }

    @GetMapping("/export")
    @Operation(summary = "导出入库单列表")
    public List<PurchaseInbound> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return purchaseInboundService.exportList(keyword, supplierId, orderId, warehouseId, status);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加入库明细")
    public PurchaseInboundItem addItem(@PathVariable Long id, @RequestBody PurchaseInboundItemDTO dto) {
        PurchaseInboundItem item = new PurchaseInboundItem();
        BeanUtils.copyProperties(dto, item);
        return purchaseInboundService.addItem(id, item);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新入库明细")
    public PurchaseInboundItem updateItem(@PathVariable Long itemId, @RequestBody PurchaseInboundItemDTO dto) {
        PurchaseInboundItem item = new PurchaseInboundItem();
        BeanUtils.copyProperties(dto, item);
        return purchaseInboundService.updateItem(itemId, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除入库明细")
    public void removeItem(@PathVariable Long itemId) {
        purchaseInboundService.removeItem(itemId);
    }

    @GetMapping("/statistics")
    @Operation(summary = "入库统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (InboundStatus status : InboundStatus.values()) {
            stats.put(status.getDesc(), purchaseInboundService.lambdaQuery()
                    .eq(PurchaseInbound::getStatus, status.getCode())
                    .eq(PurchaseInbound::getDeleted, 0)
                    .count());
        }
        stats.put("totalInboundAmount", purchaseInboundMapper.sumInboundAmount(1L));
        return stats;
    }

    private PurchaseInboundVO convertToVO(PurchaseInbound inbound) {
        PurchaseInboundVO vo = new PurchaseInboundVO();
        BeanUtils.copyProperties(inbound, vo);
        for (InboundStatus status : InboundStatus.values()) {
            if (status.getCode().equals(inbound.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}