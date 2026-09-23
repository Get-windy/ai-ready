package cn.aiedge.erp.payment.controller;

import cn.aiedge.erp.payment.dto.ReceiptCreateDTO;
import cn.aiedge.erp.payment.dto.ReceiptItemDTO;
import cn.aiedge.common.exception.BusinessException;
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
import java.util.*;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/receipt")
@RequiredArgsConstructor
@Tag(name = "收款管理", description = "收款单创建、审批、核销、完成等操作")
public class ReceiptController {

    private final ReceiptService receiptService;

    @SaCheckPermission("finance:receipt:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询收款单")
    public Page<ReceiptVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "来源类型") @RequestParam(required = false) String sourceType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "结算单位") @RequestParam(required = false) String customerName,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String departmentName,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "记账人") @RequestParam(required = false) String bookkeeperName,
            @Parameter(description = "单据备注") @RequestParam(required = false) String remark,
            @Parameter(description = "多状态(逗号分隔,待确认款项)") @RequestParam(required = false) String statuses,
            @Parameter(description = "单据编号") @RequestParam(required = false) String receiptNo,
            @Parameter(description = "来源订单编号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "配送任务编号") @RequestParam(required = false) String deliveryNo,
            @Parameter(description = "收款账户1") @RequestParam(required = false) String receiptAccount1,
            @Parameter(description = "收款账户2") @RequestParam(required = false) String receiptAccount2,
            @Parameter(description = "显示红冲") @RequestParam(required = false) Integer showRed,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Receipt> page = receiptService.pageListPending(keyword, customerId, orderId, status, sourceType,
                startDate, endDate, customerName, handlerName, departmentName, creatorName, bookkeeperName, remark,
                statuses, receiptNo, orderNo, deliveryNo, receiptAccount1, receiptAccount2,
                pageNum, pageSize);
        Page<ReceiptVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/batch-confirm")
    @Operation(summary = "批量确认待确认款项（到账入账）")
    public int batchConfirm(
            @Parameter(description = "收款单ID列表") @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请选择要确认的收款单");
        }
        int count = 0;
        for (Long id : ids) {
            receiptService.completeVerify(id);
            count++;
        }
        return count;
    }

    /**
     * 批量打印收款单。
     *
     * <p>与付款单 {@code PaymentController#batchPrint} 保持对称（同一功能在收/付两侧口径一致）。
     * 收/付两侧当前都只回执打印请求、不做模板渲染，前端 `receipt-doc/index.vue` 拿到响应后
     * 仅提示「已发送打印」。真实打印需接 `components/PrintDialog`（范本见 `views/md/barcode`、
     * `views/erp/product` 等），属独立待办，不在此处造桩。
     */
    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/batch-print")
    @Operation(summary = "批量打印收款单")
    public Map<String, Object> batchPrint(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.getOrDefault("ids", new ArrayList<>());
        String template = (String) body.getOrDefault("template", "default");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", ids == null ? 0 : ids.size());
        result.put("template", template);
        return result;
    }

    @SaCheckPermission("finance:receipt:view")
    @GetMapping("/page-detail")
    @Operation(summary = "按明细收款单分页查询（收款明细 tab）")
    public Page<cn.aiedge.erp.payment.dto.ReceiptItemDetailVO> pageDetail(
            @Parameter(description = "收款单ID") @RequestParam(required = false) Long receiptId,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "往来单位") @RequestParam(required = false) String tradeUnit,
            @Parameter(description = "源单经手人") @RequestParam(required = false) String sourceHandler,
            @Parameter(description = "结算单据编号") @RequestParam(required = false) String settlementNo,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return receiptService.pageDetail(receiptId, customerId, keyword, status, tradeUnit, sourceHandler,
                settlementNo, startDate, endDate, pageNum, pageSize);
    }

    @SaCheckPermission("finance:receipt:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成收款单号（SKD- 前缀）")
    public String nextNo() {
        return receiptService.nextNo();
    }

    @SaCheckPermission("finance:receipt:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取收款单详情")
    public ReceiptVO getById(@PathVariable Long id) {
        Receipt receipt = receiptService.getById(id);
        if (receipt == null) {
            throw BusinessException.notFound("收款单不存在");
        }
        ReceiptVO vo = convertToVO(receipt);
        vo.setItems(receiptService.getItems(id));
        return vo;
    }

    @SaCheckPermission("finance:receipt:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取收款明细")
    public List<ReceiptItem> getItems(@PathVariable Long id) {
        return receiptService.getItems(id);
    }

    @SaCheckPermission("finance:receipt:detail")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户收款单列表")
    public List<ReceiptVO> listByCustomerId(@PathVariable Long customerId) {
        return receiptService.listByCustomerId(customerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("finance:receipt:detail")
    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单收款单列表")
    public List<ReceiptVO> listByOrderId(@PathVariable Long orderId) {
        return receiptService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping
    @Operation(summary = "创建收款单")
    public ReceiptVO create(@RequestBody ReceiptCreateDTO dto) {
        Receipt receipt = new Receipt();
        BeanUtils.copyProperties(dto, receipt);
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

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从订单创建收款单")
    public ReceiptVO createFromOrder(@PathVariable Long orderId) {
        Receipt receipt = receiptService.createFromOrder(orderId);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/from-invoice/{invoiceId}")
    @Operation(summary = "从发票创建收款单")
    public ReceiptVO createFromInvoice(@PathVariable Long invoiceId) {
        Receipt receipt = receiptService.createFromInvoice(invoiceId);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:update")
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

    @SaCheckPermission("finance:receipt:submit")
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public ReceiptVO submitForApproval(@PathVariable Long id) {
        Receipt receipt = receiptService.submitForApproval(id);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:approve")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public ReceiptVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        Receipt receipt = receiptService.approve(id, approverId, note);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:approve")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public ReceiptVO reject(@PathVariable Long id, @RequestParam String reason) {
        Receipt receipt = receiptService.reject(id, reason);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/{id}/start-verify")
    @Operation(summary = "开始核销")
    public ReceiptVO startVerify(@PathVariable Long id) {
        Receipt receipt = receiptService.startVerify(id);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/{id}/items/{itemId}/verify")
    @Operation(summary = "核销明细")
    public ReceiptItem verifyItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal verifyAmount) {
        return receiptService.verifyItem(itemId, verifyAmount);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/{id}/complete-verify")
    @Operation(summary = "完成核销")
    public ReceiptVO completeVerify(@PathVariable Long id) {
        Receipt receipt = receiptService.completeVerify(id);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/{id}/write-off")
    @Operation(summary = "核销收款单")
    public ReceiptVO writeOff(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.getOrDefault("amount", BigDecimal.ZERO);
        Receipt receipt = receiptService.writeOff(id, amount);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成收款")
    public ReceiptVO complete(@PathVariable Long id) {
        Receipt receipt = receiptService.complete(id);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消收款")
    public ReceiptVO cancel(@PathVariable Long id, @RequestParam String reason) {
        Receipt receipt = receiptService.cancel(id, reason);
        return convertToVO(receipt);
    }

    @SaCheckPermission("finance:receipt:create")
    @PostMapping("/{id}/items")
    @Operation(summary = "添加收款明细")
    public ReceiptItem addItem(@PathVariable Long id, @RequestBody ReceiptItemDTO dto) {
        ReceiptItem item = new ReceiptItem();
        BeanUtils.copyProperties(dto, item);
        return receiptService.addItem(id, item);
    }

    @SaCheckPermission("finance:receipt:delete")
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除收款明细")
    public void removeItem(@PathVariable Long itemId) {
        receiptService.removeItem(itemId);
    }

    @SaCheckPermission("finance:receipt:view")
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
        List<Receipt> completedReceipts = receiptService.lambdaQuery()
                .eq(Receipt::getStatus, ReceiptStatus.COMPLETED.getCode())
                .eq(Receipt::getDeleted, 0)
                .list();
        BigDecimal totalReceiptAmount = completedReceipts.stream()
                .map(Receipt::getReceiptAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVerifiedAmount = completedReceipts.stream()
                .map(Receipt::getVerifiedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalReceiptAmount", totalReceiptAmount);
        stats.put("totalVerifiedAmount", totalVerifiedAmount);
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