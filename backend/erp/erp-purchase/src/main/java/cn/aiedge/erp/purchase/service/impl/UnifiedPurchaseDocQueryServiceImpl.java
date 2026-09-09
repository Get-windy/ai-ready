package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.erp.finance.mapper.PayableMapper;
import cn.aiedge.erp.finance.model.entity.Payable;
import cn.aiedge.erp.payment.entity.Payment;
import cn.aiedge.erp.payment.entity.PaymentItem;
import cn.aiedge.erp.payment.enums.ReceiptStatus;
import cn.aiedge.erp.payment.mapper.PaymentItemMapper;
import cn.aiedge.erp.payment.mapper.PaymentMapper;
import cn.aiedge.erp.purchase.dto.SupplierSnapshotRow;
import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocQueryDTO;
import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocumentDTO;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.service.PurchaseInboundService;
import cn.aiedge.erp.purchase.mapper.SupplierSnapshotMapper;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.service.PurchaseExchangeService;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
import cn.aiedge.erp.purchase.service.UnifiedPurchaseDocQueryService;
import cn.aiedge.erp.purchase.util.UnifiedPurchaseConverter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一采购单据查询服务实现
 * 整合采购入库单、采购退货单、采购换货单的查询（分包分页合并）。
 *
 * <p>实现说明：与销售单据查询（UnifiedSalesDocQueryServiceImpl）同构，
 * 分别 count 三种单据类型总记录数，再按全局偏移量截取合并，保证跨类型全局分页。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UnifiedPurchaseDocQueryServiceImpl implements UnifiedPurchaseDocQueryService {

    private final PurchaseInboundService purchaseInboundService;
    private final PurchaseReturnService purchaseReturnService;
    private final PurchaseExchangeService purchaseExchangeService;
    private final SupplierSnapshotMapper supplierSnapshotMapper;
    private final PayableMapper payableMapper;
    private final PaymentItemMapper paymentItemMapper;
    private final PaymentMapper paymentMapper;

    @Override
    public Page<UnifiedPurchaseDocumentDTO> unifiedPage(UnifiedPurchaseDocQueryDTO query) {
        Page<UnifiedPurchaseDocumentDTO> resultPage = new Page<>();
        if (query.getCurrent() == null || query.getCurrent() <= 0) query.setCurrent(1L);
        if (query.getSize() == null || query.getSize() <= 0) query.setSize(20L);
        resultPage.setCurrent(query.getCurrent());
        resultPage.setSize(query.getSize());

        long currentPage = query.getCurrent();
        long pageSize = query.getSize();
        String docType = query.getDocumentType();

        // Step 1: count 各类型记录数
        long inboundCount = 0, returnCount = 0, exchangeCount = 0;
        if (docType == null || docType.isEmpty() || "INBOUND".equals(docType)) {
            inboundCount = purchaseInboundService.count(buildInboundWrapper(query));
        }
        if (docType == null || docType.isEmpty() || "RETURN".equals(docType)) {
            returnCount = purchaseReturnService.count(buildReturnWrapper(query));
        }
        if (docType == null || docType.isEmpty() || "EXCHANGE".equals(docType)) {
            exchangeCount = purchaseExchangeService.count(buildExchangeWrapper(query));
        }

        long totalCount = inboundCount + returnCount + exchangeCount;
        resultPage.setTotal(totalCount);
        resultPage.setPages((totalCount + pageSize - 1) / pageSize);

        if (totalCount == 0) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        // Step 2: 全局偏移，确定各类型取数范围
        long globalStart = (currentPage - 1) * pageSize;
        long globalEnd = globalStart + pageSize;

        List<UnifiedPurchaseDocumentDTO> pageRecords = new ArrayList<>();
        long offset = 0;

        // 入库单
        if (inboundCount > 0 && offset < globalEnd) {
            long typeEnd = offset + inboundCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), inboundCount - skip);
                if (take > 0) {
                    QueryWrapper<PurchaseInbound> iw = buildInboundWrapper(query);
                    iw.orderByDesc("create_time");
                    Page<PurchaseInbound> typePage = purchaseInboundService.page(
                            new Page<>(skip / pageSize + 1, pageSize), iw);
                    List<UnifiedPurchaseDocumentDTO> converted =
                            UnifiedPurchaseConverter.convertInboundList(typePage.getRecords());
                    int startIdx = (int) (skip % pageSize);
                    if (startIdx > 0 && converted.size() > startIdx) {
                        converted = converted.subList(startIdx, converted.size());
                    }
                    int takeCount = (int) Math.min(take, converted.size());
                    if (takeCount > 0) pageRecords.addAll(converted.subList(0, takeCount));
                }
            }
            offset = typeEnd;
        }

        // 退货单
        if (returnCount > 0 && offset < globalEnd && pageRecords.size() < pageSize) {
            long typeEnd = offset + returnCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), returnCount - skip);
                if (take > 0) {
                    QueryWrapper<PurchaseReturn> rw = buildReturnWrapper(query);
                    rw.orderByDesc("create_time");
                    Page<PurchaseReturn> typePage = purchaseReturnService.page(
                            new Page<>(skip / pageSize + 1, pageSize), rw);
                    List<UnifiedPurchaseDocumentDTO> converted =
                            UnifiedPurchaseConverter.convertReturnList(typePage.getRecords());
                    int startIdx = (int) (skip % pageSize);
                    if (startIdx > 0 && converted.size() > startIdx) {
                        converted = converted.subList(startIdx, converted.size());
                    }
                    int takeCount = (int) Math.min(take, converted.size());
                    if (takeCount > 0) pageRecords.addAll(converted.subList(0, takeCount));
                }
            }
            offset = typeEnd;
        }

        // 换货单
        if (exchangeCount > 0 && offset < globalEnd && pageRecords.size() < pageSize) {
            long typeEnd = offset + exchangeCount;
            if (typeEnd > globalStart) {
                long skip = Math.max(0, globalStart - offset);
                long take = Math.min(pageSize - pageRecords.size(), exchangeCount - skip);
                if (take > 0) {
                    QueryWrapper<PurchaseExchange> ew = buildExchangeWrapper(query);
                    ew.orderByDesc("create_time");
                    Page<PurchaseExchange> typePage = purchaseExchangeService.page(
                            new Page<>(skip / pageSize + 1, pageSize), ew);
                    List<UnifiedPurchaseDocumentDTO> converted =
                            UnifiedPurchaseConverter.convertExchangeList(typePage.getRecords());
                    int startIdx = (int) (skip % pageSize);
                    if (startIdx > 0 && converted.size() > startIdx) {
                        converted = converted.subList(startIdx, converted.size());
                    }
                    int takeCount = (int) Math.min(take, converted.size());
                    if (takeCount > 0) pageRecords.addAll(converted.subList(0, takeCount));
                }
            }
            offset = typeEnd;
        }

        // Step 3: 补全入库单缺失的供应商编号/联系人/电话/地址/备注
        enrichSupplierSnapshot(pageRecords);

        // Step 4: 按单付款核销工作台 - 聚合应付核销金额(已结/待审/未结)及对账标记
        enrichPayableSettlement(pageRecords);

        resultPage.setRecords(pageRecords);
        return resultPage;
    }

    // ═══ 入库单查询条件 ═══
    private QueryWrapper<PurchaseInbound> buildInboundWrapper(UnifiedPurchaseDocQueryDTO q) {
        QueryWrapper<PurchaseInbound> w = new QueryWrapper<>();
        addCommonInbound(w, q);
        applyDateRange(w, "inbound_date", q.getDateStart(), q.getDateEnd());
        if (q.getWarehouseName() != null && !q.getWarehouseName().isEmpty()) {
            w.like("warehouse_name", q.getWarehouseName());
        }
        return w;
    }

    private void addCommonInbound(QueryWrapper<PurchaseInbound> w, UnifiedPurchaseDocQueryDTO q) {
        if (q.getDocumentNo() != null && !q.getDocumentNo().isEmpty()) w.like("inbound_no", q.getDocumentNo());
        if (q.getSupplierName() != null && !q.getSupplierName().isEmpty()) w.like("supplier_name", q.getSupplierName());
        if (q.getHandlerName() != null && !q.getHandlerName().isEmpty()) w.like("purchaser_name", q.getHandlerName());
        if (q.getDepartmentName() != null && !q.getDepartmentName().isEmpty()) w.like("department_name", q.getDepartmentName());
        if (q.getCreatorName() != null && !q.getCreatorName().isEmpty()) w.like("create_by_name", q.getCreatorName());
        if (q.getBookkeeperName() != null && !q.getBookkeeperName().isEmpty()) w.like("poster_name", q.getBookkeeperName());
        if (q.getSourceOrder() != null && !q.getSourceOrder().isEmpty()) w.like("order_no", q.getSourceOrder());
        if (q.getRemark() != null && !q.getRemark().isEmpty()) w.like("remark", q.getRemark());
        if (q.getStatus() != null) w.eq("status", q.getStatus());
        addCommonSettleAndExt(w, q.getSettlementStatus(), q.getExtNum1Start(), q.getExtNum1End(),
                q.getExtNum2Start(), q.getExtNum2End(), q.getExtText1(), q.getExtText2(), q.getExtText3());
        if (!Boolean.TRUE.equals(q.getShowRed())) w.ne("status", 3);
    }

    // ═══ 退货单查询条件 ═══
    private QueryWrapper<PurchaseReturn> buildReturnWrapper(UnifiedPurchaseDocQueryDTO q) {
        QueryWrapper<PurchaseReturn> w = new QueryWrapper<>();
        if (q.getDocumentNo() != null && !q.getDocumentNo().isEmpty()) w.like("return_no", q.getDocumentNo());
        if (q.getSupplierName() != null && !q.getSupplierName().isEmpty()) w.like("supplier_name", q.getSupplierName());
        if (q.getSupplierCode() != null && !q.getSupplierCode().isEmpty()) w.like("supplier_no", q.getSupplierCode());
        if (q.getHandlerName() != null && !q.getHandlerName().isEmpty()) w.like("purchaser_name", q.getHandlerName());
        if (q.getDepartmentName() != null && !q.getDepartmentName().isEmpty()) w.like("department_name", q.getDepartmentName());
        if (q.getCreatorName() != null && !q.getCreatorName().isEmpty()) w.like("create_by_name", q.getCreatorName());
        if (q.getBookkeeperName() != null && !q.getBookkeeperName().isEmpty()) w.like("poster_name", q.getBookkeeperName());
        if (q.getSourceOrder() != null && !q.getSourceOrder().isEmpty()) w.like("purchase_order_no", q.getSourceOrder());
        if (q.getRemark() != null && !q.getRemark().isEmpty()) w.like("remark", q.getRemark());
        if (q.getStatus() != null) w.eq("status", q.getStatus());
        applyDateRange(w, "return_date", q.getDateStart(), q.getDateEnd());
        if (q.getWarehouseName() != null && !q.getWarehouseName().isEmpty()) w.like("warehouse_name", q.getWarehouseName());
        addCommonSettleAndExt(w, q.getSettlementStatus(), q.getExtNum1Start(), q.getExtNum1End(),
                q.getExtNum2Start(), q.getExtNum2End(), q.getExtText1(), q.getExtText2(), q.getExtText3());
        if (!Boolean.TRUE.equals(q.getShowRed())) w.ne("status", 3);
        return w;
    }

    // ═══ 换货单查询条件 ═══
    private QueryWrapper<PurchaseExchange> buildExchangeWrapper(UnifiedPurchaseDocQueryDTO q) {
        QueryWrapper<PurchaseExchange> w = new QueryWrapper<>();
        if (q.getDocumentNo() != null && !q.getDocumentNo().isEmpty()) w.like("exchange_no", q.getDocumentNo());
        if (q.getSupplierName() != null && !q.getSupplierName().isEmpty()) w.like("supplier_name", q.getSupplierName());
        if (q.getSupplierCode() != null && !q.getSupplierCode().isEmpty()) w.like("supplier_code", q.getSupplierCode());
        if (q.getHandlerName() != null && !q.getHandlerName().isEmpty()) w.like("handler_name", q.getHandlerName());
        if (q.getDepartmentName() != null && !q.getDepartmentName().isEmpty()) w.like("dept_name", q.getDepartmentName());
        if (q.getCreatorName() != null && !q.getCreatorName().isEmpty()) w.like("created_by_name", q.getCreatorName());
        if (q.getBookkeeperName() != null && !q.getBookkeeperName().isEmpty()) w.like("bookkeeper_name", q.getBookkeeperName());
        if (q.getSourceOrder() != null && !q.getSourceOrder().isEmpty()) w.like("original_order_no", q.getSourceOrder());
        if (q.getRemark() != null && !q.getRemark().isEmpty()) w.like("remark", q.getRemark());
        if (q.getStatus() != null) w.eq("status", q.getStatus());
        applyDateRange(w, "exchange_date", q.getDateStart(), q.getDateEnd());
        if (q.getWarehouseName() != null && !q.getWarehouseName().isEmpty()) {
            w.and(x -> x.like("in_warehouse_name", q.getWarehouseName())
                    .or().like("out_warehouse_name", q.getWarehouseName()));
        }
        // 换货单结算状态为字符串
        if (q.getSettlementStatus() != null && !q.getSettlementStatus().isEmpty()) {
            w.eq("settle_status", q.getSettlementStatus());
        }
        if (q.getExtText1() != null && !q.getExtText1().isEmpty()) w.like("ext_text1", q.getExtText1());
        if (q.getExtText2() != null && !q.getExtText2().isEmpty()) w.like("ext_text2", q.getExtText2());
        if (q.getExtText3() != null && !q.getExtText3().isEmpty()) w.like("ext_text3", q.getExtText3());
        if (!Boolean.TRUE.equals(q.getShowRed())) w.ne("status", 3);
        return w;
    }

    /** 入库/退货单共用：结算状态 + 自定义字段 */
    private void addCommonSettleAndExt(QueryWrapper<?> w, String settleStatus,
                                       BigDecimal extNum1Start, BigDecimal extNum1End,
                                       BigDecimal extNum2Start, BigDecimal extNum2End,
                                       String extText1, String extText2, String extText3) {
        if (settleStatus != null && !settleStatus.isEmpty()) {
            Integer v = "已结算".equals(settleStatus) ? 1 : 0;
            w.eq("settle_status", v);
        }
        if (extNum1Start != null) w.ge("ext_num1", extNum1Start);
        if (extNum1End != null) w.le("ext_num1", extNum1End);
        if (extNum2Start != null) w.ge("ext_num2", extNum2Start);
        if (extNum2End != null) w.le("ext_num2", extNum2End);
        if (extText1 != null && !extText1.isEmpty()) w.like("ext_text1", extText1);
        if (extText2 != null && !extText2.isEmpty()) w.like("ext_text2", extText2);
        if (extText3 != null && !extText3.isEmpty()) w.like("ext_text3", extText3);
    }

    /** 各表日期列不同，用列名动态应用日期范围（转 LocalDate 比较，避免 varchar 与 date 类型不匹配） */
    private void applyDateRange(QueryWrapper<?> w, String dateColumn, String start, String end) {
        LocalDate startDate = parseDate(start);
        LocalDate endDate = parseDate(end);
        if (startDate != null) w.ge(dateColumn, startDate);
        if (endDate != null) w.le(dateColumn, endDate);
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            String d = s.trim();
            if (d.length() >= 10) d = d.substring(0, 10);
            return LocalDate.parse(d);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 补全入库单等缺失的供应商快照字段 */
    private void enrichSupplierSnapshot(List<UnifiedPurchaseDocumentDTO> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> ids = records.stream()
                .map(UnifiedPurchaseDocumentDTO::getSupplierId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, SupplierSnapshotRow> map = supplierSnapshotMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(SupplierSnapshotRow::getId, r -> r, (a, b) -> a));
        for (UnifiedPurchaseDocumentDTO r : records) {
            if (r.getSupplierId() == null) continue;
            SupplierSnapshotRow row = map.get(r.getSupplierId());
            if (row == null) continue;
            if (isEmpty(r.getSupplierCode())) r.setSupplierCode(row.getSupplierCode());
            if (isEmpty(r.getContactName())) r.setContactName(row.getContactName());
            if (isEmpty(r.getContactPhone())) r.setContactPhone(row.getContactPhone());
            if (isEmpty(r.getContactAddress())) r.setContactAddress(row.getContactAddress());
            if (isEmpty(r.getSupplierRemark())) r.setSupplierRemark(row.getSupplierRemark());
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 按单付款核销工作台：为采购来源单据聚合应付核销金额（已结/待审/未结）与对账标记。
     *
     * <p>关联链：采购单据(id) → finance_payable(source_id=PURCHASE_RECEIPT) →
     * erp_payment_item(payable_id) → erp_payment(status)。
     * 金额口径：本单金额 = 已结金额 + 待审金额 + 未结金额。
     * <ul>
     *   <li>待审金额 = 已提交未记账（status∈{1,2,4,5}）付款单明细金额</li>
     *   <li>已结金额 = 已核销/已记账（status∈{6,7}）付款单明细核销金额</li>
     *   <li>未结金额 = 本单金额 - 已结 - 待审（下限 0）</li>
     * </ul>
     * 对账标记(√/否)取自 finance_payable.reconcile_flag / reconcile_by_name / reconcile_at。
     */
    private void enrichPayableSettlement(List<UnifiedPurchaseDocumentDTO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 1. 收集单据ID与单据类型（仅入库单产生 finance_payable，避免跨单据类型 ID 冲突）
        Map<Long, UnifiedPurchaseDocumentDTO> inboundById = new HashMap<>();
        for (UnifiedPurchaseDocumentDTO r : records) {
            if (r.getId() != null && "INBOUND".equals(r.getDocumentType())) {
                inboundById.put(r.getId(), r);
            }
        }
        if (inboundById.isEmpty()) {
            fillZeroForAll(records);
            return;
        }

        List<Long> docIds = new ArrayList<>(inboundById.keySet());
        // 2. 查应付（采购入库单记账产生）
        List<Payable> payables = payableMapper.selectList(new QueryWrapper<Payable>()
                .in("source_id", docIds)
                .eq("source_type", "PURCHASE_RECEIPT")
                .eq("deleted_flag", 0));
        if (payables.isEmpty()) {
            fillZeroForAll(records);
            return;
        }

        Map<Long, Payable> payableBySourceId = payables.stream()
                .filter(p -> p.getSourceId() != null)
                .collect(Collectors.toMap(Payable::getSourceId, p -> p, (a, b) -> a));

        List<Long> payableIds = payables.stream()
                .map(Payable::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 3. 查付款单明细（按应付关联）
        List<PaymentItem> items = payableIds.isEmpty() ? List.of()
                : paymentItemMapper.selectList(new QueryWrapper<PaymentItem>()
                        .in("payable_id", payableIds)
                        .eq("deleted", 0));

        // 4. 查付款单（按支付状态区分已结/待审）
        Set<Long> paymentIds = items.stream()
                .map(PaymentItem::getPaymentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Payment> paymentById = new HashMap<>();
        if (!paymentIds.isEmpty()) {
            paymentById = paymentMapper.selectBatchIds(paymentIds).stream()
                    .collect(Collectors.toMap(Payment::getId, p -> p, (a, b) -> a));
        }

        // 5. 按应付ID分组明细
        Map<Long, List<PaymentItem>> itemsByPayable = new HashMap<>();
        for (PaymentItem item : items) {
            if (item.getPayableId() != null) {
                itemsByPayable.computeIfAbsent(item.getPayableId(), k -> new ArrayList<>()).add(item);
            }
        }

        // 6. 计算每张单据
        for (UnifiedPurchaseDocumentDTO r : records) {
            Payable payable = r.getId() == null ? null : payableBySourceId.get(r.getId());
            if (!"INBOUND".equals(r.getDocumentType())) {
                payable = null;
            }
            BigDecimal totalAmount = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
            r.setTotalAmount(totalAmount);

            if (payable == null) {
                setSettlement(r, BigDecimal.ZERO, BigDecimal.ZERO, totalAmount, false, null, null);
                continue;
            }

            boolean reconciled = payable.getReconcileFlag() != null && payable.getReconcileFlag() == 1;
            r.setReconcile(reconciled);
            r.setLastReconcileBy(payable.getReconcileByName());
            r.setLastReconcileTime(payable.getReconcileAt());
            r.setSettlementUnit(payable.getSupplierName());
            if (payable.getInvoiceNo() != null && !payable.getInvoiceNo().isEmpty()) {
                r.setInvoiceNumber(payable.getInvoiceNo());
            }

            BigDecimal settled = BigDecimal.ZERO;
            BigDecimal pending = BigDecimal.ZERO;
            List<PaymentItem> payableItems = itemsByPayable.getOrDefault(payable.getId(), List.of());
            for (PaymentItem item : payableItems) {
                Payment payment = item.getPaymentId() == null ? null : paymentById.get(item.getPaymentId());
                if (payment == null || payment.getStatus() == null) {
                    continue;
                }
                int st = payment.getStatus();
                if (st == ReceiptStatus.VERIFIED.getCode() || st == ReceiptStatus.COMPLETED.getCode()) {
                    BigDecimal v = item.getVerifiedAmount();
                    if (v != null) settled = settled.add(v);
                } else if (st == ReceiptStatus.PENDING_APPROVAL.getCode()
                        || st == ReceiptStatus.APPROVED.getCode()
                        || st == ReceiptStatus.PENDING_VERIFY.getCode()
                        || st == ReceiptStatus.VERIFYING.getCode()) {
                    BigDecimal pv = item.getPendingAmount() != null ? item.getPendingAmount()
                            : (item.getInvoiceAmount() != null ? item.getInvoiceAmount() : BigDecimal.ZERO);
                    if (pv != null) pending = pending.add(pv);
                }
            }
            BigDecimal unsettled = totalAmount.subtract(settled).subtract(pending);
            if (unsettled.compareTo(BigDecimal.ZERO) < 0) {
                unsettled = BigDecimal.ZERO;
            }
            setSettlement(r, settled, pending, unsettled, reconciled,
                    payable.getReconcileByName(), payable.getReconcileAt());
        }
    }

    private void fillZeroForAll(List<UnifiedPurchaseDocumentDTO> records) {
        for (UnifiedPurchaseDocumentDTO r : records) {
            BigDecimal totalAmount = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
            r.setTotalAmount(totalAmount);
            setSettlement(r, BigDecimal.ZERO, BigDecimal.ZERO, totalAmount, false, null, null);
        }
    }

    private void setSettlement(UnifiedPurchaseDocumentDTO r, BigDecimal settled, BigDecimal pending,
                               BigDecimal unsettled, boolean reconciled, String reconcileBy,
                               LocalDateTime reconcileTime) {
        r.setSettledAmount(settled);
        r.setPendingApproveAmount(pending);
        r.setUnsettledAmount(unsettled);
        r.setReconcile(reconciled);
        r.setLastReconcileBy(reconcileBy);
        r.setLastReconcileTime(reconcileTime);
    }
}
