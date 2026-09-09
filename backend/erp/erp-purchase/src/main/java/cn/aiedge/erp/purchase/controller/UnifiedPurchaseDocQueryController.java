package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.finance.mapper.PayableMapper;
import cn.aiedge.erp.finance.model.entity.Payable;
import cn.aiedge.erp.payment.entity.PaymentItem;
import cn.aiedge.erp.payment.mapper.PaymentItemMapper;
import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocQueryDTO;
import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocumentDTO;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.service.PurchaseInboundService;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.service.PurchaseExchangeService;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
import cn.aiedge.erp.purchase.service.UnifiedPurchaseDocQueryService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购单据查询控制器（统一）
 * 提供采购入库单/退货单/换货单的合并查询与整单备注更新。
 *
 * <p>区别于 {@link PurchaseDocQueryController}（采购订单-按单据Tab），
 * 本控制器路由为 {@code /api/purchase/doc-query}，服务采购单据查询页，
 * 并承载「按单付款」应付款核销工作台的待付款/全部单据查询与对账/无结算核销操作。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购单据查询(统一)", description = "入库/退货/换货合并分页查询 + 整单备注 + 按单付款核销操作")
@RestController
@RequestMapping("/api/purchase/doc-query")
@SaCheckLogin
@RequiredArgsConstructor
public class UnifiedPurchaseDocQueryController {

    private final UnifiedPurchaseDocQueryService unifiedPurchaseDocQueryService;
    private final PurchaseInboundService purchaseInboundService;
    private final PurchaseReturnService purchaseReturnService;
    private final PurchaseExchangeService purchaseExchangeService;
    private final PayableMapper payableMapper;
    private final PaymentItemMapper paymentItemMapper;

    @Operation(summary = "统一采购单据分页查询")
    @GetMapping("/page")
    public Page<UnifiedPurchaseDocumentDTO> unifiedPage(UnifiedPurchaseDocQueryDTO query) {
        return unifiedPurchaseDocQueryService.unifiedPage(query);
    }

    @Operation(summary = "更新单据整单备注")
    @PutMapping("/{docType}/{id}/remark")
    public Map<String, Object> updateRemark(
            @PathVariable String docType,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String remark = body.get("remark");
        boolean updated = false;
        switch (docType) {
            case "INBOUND": {
                PurchaseInbound e = purchaseInboundService.getById(id);
                if (e != null) { e.setRemark(remark); updated = purchaseInboundService.updateById(e); }
                break;
            }
            case "RETURN": {
                PurchaseReturn e = purchaseReturnService.getById(id);
                if (e != null) { e.setRemark(remark); updated = purchaseReturnService.updateById(e); }
                break;
            }
            case "EXCHANGE": {
                PurchaseExchange e = purchaseExchangeService.getById(id);
                if (e != null) { e.setRemark(remark); updated = purchaseExchangeService.updateById(e); }
                break;
            }
            default:
                throw new IllegalArgumentException("不支持的单据类型: " + docType);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", updated);
        res.put("message", updated ? "更新成功" : "更新失败");
        return res;
    }

    /**
     * 对账标记（按单付款）：将采购单据对应的应付标记为已对账/取消，记录最后对账标记人/时间。
     * 对账字段存放在 finance_payable(reconcile_flag/reconcile_by_name/reconcile_at) 上。
     *
     * @param documentType 单据类型（INBOUND/RETURN/EXCHANGE）
     * @param id           单据ID
     * @param flag         对账标记：1-√ 0-否（缺省则取反）
     * @param operator     对账人（前端传当前登录用户名）
     * @return 处理结果
     */
    @PutMapping("/{documentType}/{id}/reconcile")
    @Operation(summary = "按单付款-对账标记")
    public Map<String, Object> reconcile(
            @PathVariable String documentType,
            @PathVariable Long id,
            @RequestParam(required = false) Integer flag,
            @RequestParam(required = false) String operator) {
        Map<String, Object> res = new LinkedHashMap<>();
        if (!"INBOUND".equals(documentType)) {
            res.put("success", false);
            res.put("message", "仅采购入库单支持对账标记（应付已记账）");
            return res;
        }
        Payable payable = payableMapper.selectOne(new QueryWrapper<Payable>()
                .eq("source_type", "PURCHASE_RECEIPT")
                .eq("source_id", id)
                .eq("deleted_flag", 0));
        if (payable == null) {
            res.put("success", false);
            res.put("message", "该单据未生成应付记录，无法对账");
            return res;
        }
        boolean reconciled = flag != null ? flag == 1
                : (payable.getReconcileFlag() == null || payable.getReconcileFlag() != 1);
        payable.setReconcileFlag(reconciled ? 1 : 0);
        payable.setReconcileByName(operator);
        payable.setReconcileAt(LocalDateTime.now());
        payableMapper.updateById(payable);
        res.put("success", true);
        res.put("reconciled", reconciled);
        res.put("reconcileBy", operator);
        res.put("reconcileAt", payable.getReconcileAt());
        res.put("message", reconciled ? "已标记对账" : "已取消对账");
        return res;
    }

    /**
     * 无结算付款单核销（按单付款）：对已记账未勾选核销单据的付款单事后补核销。
     * 对勾选单据，若其应付已付清（remaining<=0），则补核销标记：将明细核销金额计入、单据置为已结算、应付置为已核销。
     *
     * @param docIds 勾选的采购入库单ID列表
     * @return 处理结果（processed=补核销的付款单明细条数）
     */
    @PostMapping("/no-settle-write-off")
    @Operation(summary = "按单付款-无结算付款单核销")
    public Map<String, Object> noSettleWriteOff(@RequestBody List<Long> docIds) {
        Map<String, Object> res = new LinkedHashMap<>();
        if (docIds == null || docIds.isEmpty()) {
            res.put("success", false);
            res.put("message", "请先勾选需要核销的单据");
            return res;
        }
        int processed = 0;
        int updatedDocs = 0;
        for (Long docId : docIds) {
            Payable payable = payableMapper.selectOne(new QueryWrapper<Payable>()
                    .eq("source_type", "PURCHASE_RECEIPT")
                    .eq("source_id", docId)
                    .eq("deleted_flag", 0));
            if (payable == null || payable.getTotalAmount() == null) {
                continue;
            }
            // 仅对付清（remaining<=0 或剩余不小于已核销）的单据补核销，避免越过付款单直接改金额
            BigDecimal paid = payable.getPaidAmount() != null ? payable.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal total = payable.getTotalAmount();
            boolean cleared = paid.compareTo(total) >= 0
                    || (payable.getRemainingAmount() != null && payable.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0);
            if (!cleared) {
                continue;
            }
            // 补核销：把该应付下已记账未标记核销的明细标记为已核销并计入已核销金额
            List<PaymentItem> items = paymentItemMapper.selectList(new QueryWrapper<PaymentItem>()
                    .eq("payable_id", payable.getId())
                    .eq("deleted", 0));
            for (PaymentItem item : items) {
                if (item.getVerifyStatus() != null && item.getVerifyStatus() == 1) {
                    continue;
                }
                BigDecimal amount = item.getInvoiceAmount() != null ? item.getInvoiceAmount()
                        : (item.getOrderAmount() != null ? item.getOrderAmount() : BigDecimal.ZERO);
                item.setVerifyStatus(1);
                if (item.getVerifiedAmount() == null) {
                    item.setVerifiedAmount(amount);
                }
                paymentItemMapper.updateById(item);
                processed++;
            }
            // 单据置为已结算
            PurchaseInbound inbound = purchaseInboundService.getById(docId);
            if (inbound != null && (inbound.getSettleStatus() == null || inbound.getSettleStatus() != 1)) {
                inbound.setSettleStatus(1);
                inbound.setSettledAmount(total);
                purchaseInboundService.updateById(inbound);
                updatedDocs++;
            }
            // 应付置为已核销
            payable.setStatus("written_off");
            payable.setRemainingAmount(BigDecimal.ZERO);
            payable.setPaidAmount(total);
            payableMapper.updateById(payable);
        }
        res.put("success", true);
        res.put("processed", processed);
        res.put("updatedDocs", updatedDocs);
        res.put("message", "已补核销 " + processed + " 条付款明细 / " + updatedDocs + " 张单据");
        return res;
    }
}
