package cn.aiedge.erp.sale.return.controller;

import cn.aiedge.erp.sale.return.dto.SaleReturnCreateDTO;
import cn.aiedge.erp.sale.return.dto.SaleReturnItemDTO;
import cn.aiedge.erp.sale.return.dto.SaleReturnVO;
import cn.aiedge.erp.sale.return.entity.SaleReturn;
import cn.aiedge.erp.sale.return.entity.SaleReturnItem;
import cn.aiedge.erp.sale.return.enums.ReturnStatus;
import cn.aiedge.erp.sale.return.service.SaleReturnService;
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
@RequestMapping("/api/erp/sale-return")
@RequiredArgsConstructor
@Tag(name = "销售退货管理", description = "销售退货申请、审批、收货、入库、退款等操作")
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    @GetMapping("/page")
    @Operation(summary = "分页查询退货单")
    public Page<SaleReturnVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "退货类型") @RequestParam(required = false) Integer returnType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<SaleReturn> page = saleReturnService.pageList(keyword, customerId, orderId, status, returnType, pageNum, pageSize);
        Page<SaleReturnVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取退货单详情")
    public SaleReturnVO getById(@PathVariable Long id) {
        SaleReturn returnOrder = saleReturnService.getById(id);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        SaleReturnVO vo = convertToVO(returnOrder);
        vo.setItems(saleReturnService.getItems(id));
        return vo;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取退货明细")
    public List<SaleReturnItem> getItems(@PathVariable Long id) {
        return saleReturnService.getItems(id);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户的退货单列表")
    public List<SaleReturnVO> listByCustomerId(@PathVariable Long customerId) {
        return saleReturnService.listByCustomerId(customerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单的退货单列表")
    public List<SaleReturnVO> listByOrderId(@PathVariable Long orderId) {
        return saleReturnService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "创建退货单")
    public SaleReturnVO create(@RequestBody SaleReturnCreateDTO dto) {
        SaleReturn returnOrder = new SaleReturn();
        BeanUtils.copyProperties(dto, returnOrder);
        returnOrder.setTenantId(1L);
        returnOrder.setCreateBy(StpUtil.getLoginIdAsLong());
        List<SaleReturnItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                SaleReturnItem item = new SaleReturnItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        SaleReturn created = saleReturnService.createReturn(returnOrder, items);
        return convertToVO(created);
    }

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从销售订单创建退货单")
    public SaleReturnVO createFromOrder(@PathVariable Long orderId) {
        SaleReturn returnOrder = saleReturnService.createFromOrder(orderId);
        return convertToVO(returnOrder);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新退货单")
    public SaleReturnVO update(@PathVariable Long id, @RequestBody SaleReturnCreateDTO dto) {
        SaleReturn returnOrder = new SaleReturn();
        BeanUtils.copyProperties(dto, returnOrder);
        List<SaleReturnItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                SaleReturnItem item = new SaleReturnItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        SaleReturn updated = saleReturnService.updateReturn(id, returnOrder, items);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public SaleReturnVO submitForApproval(@PathVariable Long id) {
        SaleReturn returnOrder = saleReturnService.submitForApproval(id);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public SaleReturnVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        SaleReturn returnOrder = saleReturnService.approve(id, approverId, note);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public SaleReturnVO reject(@PathVariable Long id, @RequestParam String reason) {
        Long rejecterId = StpUtil.getLoginIdAsLong();
        SaleReturn returnOrder = saleReturnService.reject(id, rejecterId, reason);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "收货")
    public SaleReturnVO receive(@PathVariable Long id) {
        Long receiverId = StpUtil.getLoginIdAsLong();
        SaleReturn returnOrder = saleReturnService.receive(id, receiverId);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/items/{itemId}/receive")
    @Operation(summary = "收货明细处理")
    public SaleReturnItem receiveItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal acceptedQuantity,
            @RequestParam BigDecimal rejectedQuantity,
            @RequestParam(required = false) String qualityNote) {
        return saleReturnService.receiveItem(itemId, acceptedQuantity, rejectedQuantity, qualityNote);
    }

    @PostMapping("/{id}/warehouse-confirm")
    @Operation(summary = "确认入库")
    public SaleReturnVO confirmWarehouse(@PathVariable Long id) {
        Long confirmerId = StpUtil.getLoginIdAsLong();
        SaleReturn returnOrder = saleReturnService.confirmWarehouse(id, confirmerId);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/process-refund")
    @Operation(summary = "处理退款")
    public SaleReturnVO processRefund(
            @PathVariable Long id,
            @RequestParam String refundMethod,
            @RequestParam(required = false) String refundAccount,
            @RequestParam(required = false) String refundNote) {
        SaleReturn returnOrder = saleReturnService.processRefund(id, refundMethod, refundAccount, refundNote);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/complete-refund")
    @Operation(summary = "完成退款")
    public SaleReturnVO completeRefund(@PathVariable Long id) {
        SaleReturn returnOrder = saleReturnService.completeRefund(id);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成退货")
    public SaleReturnVO complete(@PathVariable Long id) {
        SaleReturn returnOrder = saleReturnService.complete(id);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消退货")
    public SaleReturnVO cancel(@PathVariable Long id, @RequestParam String reason) {
        SaleReturn returnOrder = saleReturnService.cancel(id, reason);
        return convertToVO(returnOrder);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加退货明细")
    public SaleReturnItem addItem(@PathVariable Long id, @RequestBody SaleReturnItemDTO dto) {
        SaleReturnItem item = new SaleReturnItem();
        BeanUtils.copyProperties(dto, item);
        return saleReturnService.addItem(id, item);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新退货明细")
    public SaleReturnItem updateItem(@PathVariable Long itemId, @RequestBody SaleReturnItemDTO dto) {
        SaleReturnItem item = new SaleReturnItem();
        BeanUtils.copyProperties(dto, item);
        return saleReturnService.updateItem(itemId, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除退货明细")
    public void removeItem(@PathVariable Long itemId) {
        saleReturnService.removeItem(itemId);
    }

    @GetMapping("/statistics")
    @Operation(summary = "退货统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (ReturnStatus status : ReturnStatus.values()) {
            stats.put(status.getDesc(), saleReturnService.lambdaQuery()
                    .eq(SaleReturn::getStatus, status.getCode())
                    .eq(SaleReturn::getDeleted, 0)
                    .count());
        }
        stats.put("totalRefundAmount", saleReturnService.baseMapper.sumRefundAmount(1L));
        return stats;
    }

    private SaleReturnVO convertToVO(SaleReturn returnOrder) {
        SaleReturnVO vo = new SaleReturnVO();
        BeanUtils.copyProperties(returnOrder, vo);
        for (ReturnStatus status : ReturnStatus.values()) {
            if (status.getCode().equals(returnOrder.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}