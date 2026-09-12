package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.sale.dto.AccountDeliveryDocVO;
import cn.aiedge.erp.sale.dto.AccountDeliveryQueryDTO;
import cn.aiedge.erp.sale.dto.AccountDeliveryResult;
import cn.aiedge.erp.sale.dto.AccountDeliveryStaffVO;
import cn.aiedge.erp.sale.dto.AccountDeliverySummaryDTO;
import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import cn.aiedge.erp.sale.service.AccountDeliveryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账款交账服务实现
 * 数据源：销售出库单 erp_sale_outbound（配送代收/业务员代收款项）
 * 待交账 = 结算状态非 settled（unsettled/partial），即仍有未收部分
 * 交账动作 = 将选中单据结算状态回写为 settled（款项交回入账，职员待交账清零）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeliveryServiceImpl implements AccountDeliveryService {

    private final SaleOutboundService saleOutboundService;

    @Override
    public AccountDeliveryResult docPage(AccountDeliveryQueryDTO query) {
        List<SaleOutbound> outbounds = queryOutbounds(query);
        List<AccountDeliveryDocVO> all = outbounds.stream().map(this::toDocVO).toList();

        AccountDeliverySummaryDTO summary = buildSummary(all);

        long current = query.getCurrent() != null ? query.getCurrent() : 1;
        long size = query.getSize() != null ? query.getSize() : 20;
        long total = all.size();
        int from = (int) Math.min(total, (current - 1) * size);
        int to = (int) Math.min(total, from + size);

        AccountDeliveryResult result = new AccountDeliveryResult();
        result.setRecords(all.subList(from, to));
        result.setTotal(total);
        result.setSummary(summary);
        return result;
    }

    @Override
    public AccountDeliveryResult staffList(AccountDeliveryQueryDTO query) {
        List<SaleOutbound> outbounds = queryOutbounds(query);

        // 按交账职员分组
        Map<String, List<AccountDeliveryDocVO>> grouped = new LinkedHashMap<>();
        for (SaleOutbound ob : outbounds) {
            AccountDeliveryDocVO vo = toDocVO(ob);
            grouped.computeIfAbsent(vo.getDeliverStaff(), k -> new ArrayList<>()).add(vo);
        }

        List<AccountDeliveryStaffVO> staffList = new ArrayList<>();
        for (Map.Entry<String, List<AccountDeliveryDocVO>> e : grouped.entrySet()) {
            List<AccountDeliveryDocVO> rows = e.getValue();
            AccountDeliveryStaffVO s = new AccountDeliveryStaffVO();
            s.setDeliverStaff(e.getKey());
            s.setDepartmentName(rows.stream().map(AccountDeliveryDocVO::getDepartmentName)
                    .filter(StringUtils::hasText).findFirst().orElse(null));
            s.setDocCount((long) rows.size());
            s.setTotalAmount(sum(rows, AccountDeliveryDocVO::getDueAmount));
            s.setUsedAdvance(sum(rows, AccountDeliveryDocVO::getUsedAdvance));
            s.setReceiveTotal(sum(rows, AccountDeliveryDocVO::getReceiveTotal));
            s.setFavorableAmount(sum(rows, AccountDeliveryDocVO::getFavorableAmount));
            s.setArrearsAmount(sum(rows, AccountDeliveryDocVO::getArrearsAmount));
            staffList.add(s);
        }
        // 汇总金额 + 合计行（空职员名兜底显示）
        staffList.sort((a, b) -> {
            BigDecimal ab = nz(a.getArrearsAmount());
            BigDecimal bb = nz(b.getArrearsAmount());
            return bb.compareTo(ab);
        });

        AccountDeliveryResult result = new AccountDeliveryResult();
        List<AccountDeliveryDocVO> flattened = new ArrayList<>();
        for (SaleOutbound ob : outbounds) {
            flattened.add(toDocVO(ob));
        }
        result.setStaffList(staffList);
        result.setSummary(buildSummary(flattened));
        result.setTotal((long) outbounds.size());
        return result;
    }

    @Override
    @Transactional
    public void deliver(AccountDeliveryQueryDTO query) {
        List<SaleOutbound> target = new ArrayList<>();
        if ("OUTBOUND".equalsIgnoreCase(query.getSourceType()) && query.getSourceIds() != null && !query.getSourceIds().isEmpty()) {
            target = saleOutboundService.listByIds(query.getSourceIds());
        } else if (StringUtils.hasText(query.getDeliverStaff())) {
            QueryWrapper<SaleOutbound> w = buildBaseWrapper(query);
            w.and(x -> x.eq("delivery_driver", query.getDeliverStaff())
                    .or().eq("sales_person_name", query.getDeliverStaff()));
            target = saleOutboundService.list(w);
        } else {
            throw new IllegalArgumentException("交账动作必须指定 sourceType+sourceIds 或 deliverStaff");
        }

        for (SaleOutbound ob : target) {
            if (ob.getSettlementStatus() != null && "settled".equals(ob.getSettlementStatus())) {
                continue; // 已交账，跳过
            }
            SaleOutbound update = new SaleOutbound();
            update.setId(ob.getId());
            update.setSettlementStatus("settled");
            update.setSettledAmount(ob.getTotalAmount() != null ? ob.getTotalAmount() : ob.getSettledAmount());
            update.setReconciliationDate(LocalDate.now());
            update.setPaymentDate(ob.getPaymentDate() != null ? ob.getPaymentDate() : LocalDate.now());
            saleOutboundService.updateById(update);
        }
    }

    // ═══════════ 私有辅助 ═══════════

    /**
     * 查询待交账出库单：结算状态非 settled + 日期 + 查询条件
     */
    private List<SaleOutbound> queryOutbounds(AccountDeliveryQueryDTO query) {
        QueryWrapper<SaleOutbound> w = buildBaseWrapper(query);

        // 待交账 = 结算状态非 settled（unsettled/partial）
        w.ne("settlement_status", "settled");
        w.apply("COALESCE(settled_amount,0) < total_amount");

        // 交账职员筛选
        if (StringUtils.hasText(query.getStaffName())) {
            w.and(x -> x.like("delivery_driver", query.getStaffName())
                    .or().like("sales_person_name", query.getStaffName()));
        }

        // 客户
        if (StringUtils.hasText(query.getCustomerName())) {
            w.like("customer_name", query.getCustomerName());
        }
        // 单据编号
        if (StringUtils.hasText(query.getDocumentNo())) {
            w.like("outbound_no", query.getDocumentNo());
        }
        // 业务经手人
        if (StringUtils.hasText(query.getHandlerName())) {
            w.like("sales_person_name", query.getHandlerName());
        }
        // 结算单位 / 客户结款方式
        if (StringUtils.hasText(query.getSettlementUnit()) || StringUtils.hasText(query.getSettlementMethod())) {
            String v = StringUtils.hasText(query.getSettlementUnit()) ? query.getSettlementUnit() : query.getSettlementMethod();
            w.eq("settlement_method", v);
        }
        // 收款账户
        if (StringUtils.hasText(query.getReceiptAccount())) {
            w.and(x -> x.eq("payment_account1", query.getReceiptAccount())
                    .or().eq("payment_account2", query.getReceiptAccount())
                    .or().eq("payment_account3", query.getReceiptAccount())
                    .or().eq("payment_account4", query.getReceiptAccount()));
        }
        // 配送任务编号
        if (StringUtils.hasText(query.getDeliveryTaskNo())) {
            w.like("delivery_order_no", query.getDeliveryTaskNo());
        }
        // 单据备注
        if (StringUtils.hasText(query.getRemark())) {
            w.like("remark", query.getRemark());
        }

        w.orderByDesc("outbound_date");
        return saleOutboundService.list(w);
    }

    /**
     * 公共查询条件：租户/删除 + 日期范围
     */
    private QueryWrapper<SaleOutbound> buildBaseWrapper(AccountDeliveryQueryDTO query) {
        QueryWrapper<SaleOutbound> w = new QueryWrapper<>();
        w.eq("deleted", 0);
        if (StringUtils.hasText(query.getDateStart())) {
            LocalDate start = parseDate(query.getDateStart());
            if (start != null) w.ge("outbound_date", start);
        }
        if (StringUtils.hasText(query.getDateEnd())) {
            LocalDate end = parseDate(query.getDateEnd());
            if (end != null) w.le("outbound_date", end);
        }
        return w;
    }

    /**
     * 出库单 → 按单据 VO（金额口径：应交=本单金额；已收=已结算；优惠=收款优惠；欠款=应交-已收）
     */
    private AccountDeliveryDocVO toDocVO(SaleOutbound ob) {
        AccountDeliveryDocVO vo = new AccountDeliveryDocVO();
        vo.setId(ob.getId());
        vo.setSourceType("OUTBOUND");

        BigDecimal total = nz(ob.getTotalAmount());
        BigDecimal settled = nz(ob.getSettledAmount());
        BigDecimal favorable = nz(ob.getDirectDiscount()).add(nz(ob.getPromoDiscount()))
                .add(nz(ob.getCouponAmount())).add(nz(ob.getRoundingAmount()));
        BigDecimal arrears = total.subtract(settled).max(BigDecimal.ZERO);

        vo.setConfirmFlag(Boolean.FALSE);
        vo.setBizDate(ob.getOutboundDate());
        vo.setDocumentNo(ob.getOutboundNo());
        vo.setSettlementMethod(ob.getSettlementMethod());
        vo.setCustomerName(ob.getCustomerName());
        vo.setDeliverStaff(firstText(ob.getDeliveryDriver(), ob.getSalesPersonName()));
        vo.setDeliveryType("送货代收");
        vo.setDeliverStatus(settlementStatusLabel(ob.getSettlementStatus()));
        vo.setBusinessType("销售出库单");
        vo.setDocumentType("OUTBOUND");
        vo.setDeliveryTaskNo(ob.getDeliveryOrderNo());
        vo.setSettlementUnit(ob.getSettlementMethod());
        vo.setHandlerName(ob.getSalesPersonName());
        vo.setDueAmount(total);
        vo.setFavorableAmount(favorable);
        vo.setArrearsAmount(arrears);
        vo.setReceiptDetail(null);

        vo.setPaymentAccount1(ob.getPaymentAccount1());
        vo.setPaymentAccount2(ob.getPaymentAccount2());
        vo.setPaymentAccount3(ob.getPaymentAccount3());
        vo.setPaymentAccount4(ob.getPaymentAccount4());
        vo.setUsedAdvance(nz(ob.getUsedAdvancePayment()));
        vo.setReceiveTotal(settled);

        vo.setShipQty(ob.getTotalQuantity());
        vo.setShipAmount(total);
        vo.setReceiveQty(ob.getTotalQuantity());
        vo.setReceiveAmount(settled);
        vo.setRejectQty(BigDecimal.ZERO);
        vo.setRejectAmount(BigDecimal.ZERO);
        vo.setReturnQty(ob.getReturnQuantity());
        vo.setReturnAmount(nz(ob.getReturnAmount()));
        vo.setOrderDeposit(nz(ob.getOrderDeposit()));
        vo.setPendingDeposit(BigDecimal.ZERO);
        vo.setOriginalAdvance(nz(ob.getUsedAdvancePayment()));
        vo.setDiscountAmount(nz(ob.getDirectDiscount()).add(nz(ob.getPromoDiscount()))
                .add(nz(ob.getCouponAmount())).add(nz(ob.getRoundingAmount())));
        vo.setOtherFee(nz(ob.getOtherFee()));
        vo.setAttachments(null);
        vo.setRemark(ob.getRemark());
        vo.setDepartmentName(ob.getDepartmentName());
        vo.setPaymentDate(ob.getPaymentDate());
        vo.setReconciliationDate(ob.getReconciliationDate());
        vo.setSalesPersonId(ob.getSalesPersonId());
        return vo;
    }

    /**
     * 五档统计
     */
    private AccountDeliverySummaryDTO buildSummary(List<AccountDeliveryDocVO> rows) {
        AccountDeliverySummaryDTO s = new AccountDeliverySummaryDTO();
        s.setDocCount((long) rows.size());
        s.setTotalAmount(sum(rows, AccountDeliveryDocVO::getDueAmount));
        s.setUsedAdvance(sum(rows, AccountDeliveryDocVO::getUsedAdvance));
        s.setReceiveTotal(sum(rows, AccountDeliveryDocVO::getReceiveTotal));
        s.setFavorableAmount(sum(rows, AccountDeliveryDocVO::getFavorableAmount));
        s.setArrearsAmount(sum(rows, AccountDeliveryDocVO::getArrearsAmount));
        s.setReceiveAmount(s.getUsedAdvance().add(s.getReceiveTotal()));
        return s;
    }

    private BigDecimal sum(List<AccountDeliveryDocVO> rows, java.util.function.Function<AccountDeliveryDocVO, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (AccountDeliveryDocVO r : rows) total = total.add(nz(getter.apply(r)));
        return total;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String firstText(String... vals) {
        for (String v : vals) {
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }

    private String settlementStatusLabel(String status) {
        if (status == null) return "未交账";
        switch (status) {
            case "settled": return "已交账";
            case "partial": return "部分交账";
            default: return "未交账";
        }
    }

    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
