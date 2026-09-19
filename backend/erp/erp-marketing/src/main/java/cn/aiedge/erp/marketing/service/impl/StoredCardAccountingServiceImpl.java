package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.marketing.entity.StoredCard;
import cn.aiedge.erp.marketing.entity.StoredCardFlow;
import cn.aiedge.erp.marketing.mapper.StoredCardFlowMapper;
import cn.aiedge.erp.marketing.service.StoredCardAccountingService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link StoredCardAccountingService} 实现。
 *
 * <p>科目口径（标准科目编码，与财务域既有用法一致）：</p>
 * <table>
 *   <tr><th>业务</th><th>借方</th><th>贷方</th></tr>
 *   <tr><td>开卡 / 充值</td><td>1001 库存现金 / 1002 银行存款</td><td>2203 预收账款</td></tr>
 *   <tr><td>消费</td><td>2203 预收账款</td><td>6001 主营业务收入</td></tr>
 *   <tr><td>退款</td><td>2203 预收账款</td><td>1001 库存现金 / 1002 银行存款</td></tr>
 * </table>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoredCardAccountingServiceImpl implements StoredCardAccountingService {

    /** 库存现金 */
    private static final String SUBJECT_CASH = "1001";
    /** 银行存款 */
    private static final String SUBJECT_BANK = "1002";
    /** 预收账款（单用途预付卡形成负债） */
    private static final String SUBJECT_ADVANCE = "2203";
    /** 主营业务收入 */
    private static final String SUBJECT_REVENUE = "6001";

    private static final String SOURCE_TYPE = "STORED_CARD";
    private static final String ACCOUNT_CASH = "CASH";

    private final BusinessAccountingService businessAccountingService;
    private final StoredCardFlowMapper flowMapper;

    @Override
    public String postVoucher(StoredCard card, StoredCardFlow flow, BigDecimal amount) {
        if (flow == null || amount == null || amount.signum() <= 0) return null;
        // 幂等：已生成过就不再生成（重试/补偿安全）
        if (flow.getVoucherNo() != null && !flow.getVoucherNo().isBlank()) {
            return flow.getVoucherNo();
        }

        String settle = flow.getSettleAccount() == null ? SUBJECT_BANK : flow.getSettleAccount();
        String cashOrBank = ACCOUNT_CASH.equals(settle) ? SUBJECT_CASH : SUBJECT_BANK;
        String cashOrBankName = ACCOUNT_CASH.equals(settle) ? "库存现金" : "银行存款";

        List<BusinessAccountingRequest.AccountingRequestItem> items = new ArrayList<>();
        String summary;
        switch (flow.getFlowType()) {
            case StoredCardFlow.ISSUE, StoredCardFlow.RECHARGE -> {
                summary = ("ISSUE".equals(flow.getFlowType()) ? "储值卡开卡 - " : "储值卡充值 - ") + card.getCardNo();
                items.add(item(summary + "（收 " + cashOrBankName + "）", cashOrBank, amount, BigDecimal.ZERO));
                items.add(item(summary + "（预收）", SUBJECT_ADVANCE, BigDecimal.ZERO, amount));
            }
            case StoredCardFlow.CONSUME -> {
                summary = "储值卡消费结转 - " + card.getCardNo();
                items.add(item(summary + "（冲预收）", SUBJECT_ADVANCE, amount, BigDecimal.ZERO));
                items.add(item(summary + "（确认收入）", SUBJECT_REVENUE, BigDecimal.ZERO, amount));
            }
            case StoredCardFlow.REFUND -> {
                summary = "储值卡退款 - " + card.getCardNo();
                items.add(item(summary + "（冲预收）", SUBJECT_ADVANCE, amount, BigDecimal.ZERO));
                items.add(item(summary + "（退 " + cashOrBankName + "）", cashOrBank, BigDecimal.ZERO, amount));
            }
            default -> {
                // 赠送 / 调整不产生现金流，不生成凭证（开具口径见迁移注释）
                return null;
            }
        }

        BusinessAccountingRequest req = new BusinessAccountingRequest();
        req.setSourceType(SOURCE_TYPE);
        // 以**流水 id** 作 sourceId：一张卡多次充值是各自独立的凭证
        req.setSourceId(flow.getId());
        req.setSourceNo(flow.getVoucherNo() == null ? flow.getCardNo() : flow.getVoucherNo());
        req.setAmount(amount);
        req.setSummary(summary);
        req.setVoucherDate(LocalDate.now());
        if (card.getPartnerId() != null) {
            req.setCustomerId(String.valueOf(card.getPartnerId()));
            req.setCustomerName(card.getPartnerName());
        }
        req.setItems(items);

        try {
            VoucherDTO voucher = businessAccountingService.createVoucherFromBusiness(req);
            String voucherNo = voucher == null ? null : voucher.getVoucherNo();
            if (voucherNo != null && !voucherNo.isBlank()) {
                flowMapper.update(null, new LambdaUpdateWrapper<StoredCardFlow>()
                        .eq(StoredCardFlow::getId, flow.getId())
                        .set(StoredCardFlow::getVoucherNo, voucherNo));
                log.info("[储值卡] {} {} {} 已生成凭证 {}", card.getCardNo(), flow.getFlowType(), amount, voucherNo);
            }
            return voucherNo;
        } catch (Exception e) {
            // 凭证失败与余额变动同事务：抛出让整笔回滚，避免"钱动了账没动"
            log.error("[储值卡] {} {} {} 生成凭证失败，整笔回滚", card.getCardNo(), flow.getFlowType(), amount, e);
            throw new IllegalStateException("储值卡记账失败：" + e.getMessage(), e);
        }
    }

    private BusinessAccountingRequest.AccountingRequestItem item(String summary, String subjectCode,
                                                                 BigDecimal debit, BigDecimal credit) {
        BusinessAccountingRequest.AccountingRequestItem it = new BusinessAccountingRequest.AccountingRequestItem();
        it.setSummary(summary);
        it.setSubjectCode(subjectCode);
        it.setDebitAmount(debit);
        it.setCreditAmount(credit);
        return it;
    }
}
