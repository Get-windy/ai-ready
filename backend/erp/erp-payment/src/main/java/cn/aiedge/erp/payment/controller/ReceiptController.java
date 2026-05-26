package cn.aiedge.erp.payment.controller;

import cn.aiedge.erp.payment.dto.ReceiptCreateDTO;
import cn.aiedge.erp.payment.dto.ReceiptItemDTO;
import cn.aiedge.erp.payment.dto.ReceiptVO;
import cn.aiedge.erp.payment.entity.Receipt;
import cn.aiedge.erp.payment.entity.ReceiptItem;
import cn.aiedge.erp.payment.enums.ReceiptStatus;
import cn.aiedge.erp.payment.service.ReceiptService;
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
@RequestMapping("/api/erp/receipt")
@RequiredArgsConstructor
@Tag(name = "收款管理", description = "收款单创建、审批、核销、完成等操作")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/page")
    @Operation(summary = "分页查询收款单")
    public Page<ReceiptVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Receipt> page = receiptService.pageList(keyword, customerId, orderId, status, pageNum, pageSize);
        Page<ReceiptVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取收款单详情")
    public ReceiptVO getById(@PathVariable Long id) {
        Receipt receipt = receiptService.getById(id);
        if (receipt == null) {
            throw new RuntimeException("收款单不存在");
        }
        ReceiptVO vo = convertToVO(receipt);
        vo.setItems(receiptService.getItems(id));
        return vo;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取收款明细")
    public List<ReceiptItem> getItems(@PathVariable Long id) {
        return receiptService.getItems(id);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户收款单列表")
    public List<ReceiptVO> listByCustomerId(@PathVariable Long customerId) {
        return receiptService.listByCustomerId(customerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单收款单列表")
    public List<ReceiptVO> listByOrderId(@PathVariable Long orderId) {
        return receiptService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "创建收款单")
    public ReceiptVO create(@RequestBody ReceiptCreateDTO dto) {
        Receipt receipt = new Receipt();
        BeanUtils.copyProperties(dto, receipt);
        receipt.setTenantId(1L);
        receipt.setCreateBy(StpUtil.getLoginIdAsLong());
        List<ReceiptItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                ReceiptItem item = new ReceiptItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        Receipt created = receiptService.createReceipt(receipt, items);
        return convertToVO(created);
    }

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从订单创建收款单")
    public ReceiptVO createFromOrder(@PathVariable Long orderId) {
        Receipt receipt = receiptService.createFromOrder(orderId);
        return convertToVO(receipt);
    }

    @PostMapping("/from-invoice/{invoiceId}")
    @Operation(summary = "从发票创建收款单")
    public ReceiptVO createFromInvoice(@PathVariable Long invoiceId) {
        Receipt receipt = receiptService.createFromInvoice(invoiceId);
        return convertToVO(receipt);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新收款单")
    public ReceiptVO update(@PathVariable Long id, @RequestBody ReceiptCreateDTO dto) {
        Receipt receipt = new Receipt();
        BeanUtils.copyProperties(dto, receipt);
        List<ReceiptItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                ReceiptItem item = new ReceiptItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        Receipt updated = receiptService.updateReceipt(id, receipt, items);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public ReceiptVO submitForApproval(@PathVariable Long id) {
        Receipt receipt = receiptService.submitForApproval(id);
        return convertToVO(receipt);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public ReceiptVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        Receipt receipt = receiptService.approve(id, approverId, note);
        return convertToVO(receipt);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public ReceiptVO reject(@PathVariable Long id, @RequestParam String reason) {
        Receipt receipt = receiptService.reject(id, reason);
        return convertToVO(receipt);
    }

    @PostMapping("/{id}/start-verify")
    @Operation(summary = "开始核销")
    public ReceiptVO startVerify(@PathVariable Long id) {
        Receipt receipt = receiptService.startVerify(id);
        return convertToVO(receipt);
    }

    @PostMapping("/{id}/items/{itemId}/verify")
    @Operation(summary = "核销明细")
    public ReceiptItem verifyItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal verifyAmount) {
        return receiptService.verifyItem(itemId, verifyAmount);
    }

    @PostMapping("/{id}/complete-verify")
    @Operation(summary = "完成核销")
    public ReceiptVO completeVerify(@PathVariable Long id) {
        Receipt receipt = receiptService.completeVerify(id);
        return convertToVO(receipt);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成收款")
    public ReceiptVO complete(@PathVariable Long id) {
        Receipt receipt = receiptService.complete(id);
        return convertToVO(receipt);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消收款")
    public ReceiptVO cancel(@PathVariable Long id, @RequestParam String reason) {
        Receipt receipt = receiptService.cancel(id, reason);
        return convertToVO(receipt);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加收款明细")
    public ReceiptItem addItem(@PathVariable Long id, @RequestBody ReceiptItemDTO dto) {
        ReceiptItem item = new ReceiptItem();
        BeanUtils.copyProperties(dto, item);
        return receiptService.addItem(id, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除收款明细")
    public void removeItem(@PathVariable Long itemId) {
        receiptService.removeItem(itemId);
    }

    @GetMapping("/statistics")
    @Operation(summary = "收款统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (ReceiptStatus status : ReceiptStatus.values()) {
            stats.put(status.getDesc(), receiptService.lambdaQuery()
                    .eq(Receipt::getStatus, status.getCode())
                    .eq(Receipt::getDeleted, 0)
                    .count());
        }
        stats.put("totalReceiptAmount", receiptService.baseMapper.sumReceiptAmount(1L));
        stats.put("totalVerifiedAmount", receiptService.baseMapper.sumVerifiedAmount(1L));
        return stats;
    }

    private ReceiptVO convertToVO(Receipt receipt) {
        ReceiptVO vo = new ReceiptVO();
        BeanUtils.copyProperties(receipt, vo);
        for (ReceiptStatus status : ReceiptStatus.values()) {
            if (status.getCode().equals(receipt.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}