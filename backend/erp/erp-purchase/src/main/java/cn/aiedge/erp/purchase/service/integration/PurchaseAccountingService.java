package cn.aiedge.erp.purchase.service.integration;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.purchase.mapper.CounterpartyTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购模块记账集成服务
 * 直接调用财务模块的 {@link BusinessAccountingService} 创建应付与会计凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseAccountingService {

    private final BusinessAccountingService businessAccountingService;
    private final PayableService payableService;

    /** 取「供应商所属租户」（协议账期需要两端租户；单据上只有供应商 {@code biz_party.id}）。 */
    private final CounterpartyTenantMapper counterpartyTenantMapper;

    /**
     * 收货时创建应付和库存凭证
     * Dr. Inventory (存货)
     * Cr. Accounts Payable (应付账款)
     *
     * @param receiptId   收货单ID
     * @param receiptNo   收货单编号
     * @param supplierId  供应商ID
     * @param supplierName 供应商名称
     * @param amount      金额
     * @param businessDate <b>单据的业务日期</b>（采购收货单的 {@code inboundDate} = 收货/入库日期）。
     *                     它是「双方约定账期」的起算基准日：到期日 = 业务日期 + 约定天数。
     *                     <b>不许用 {@link LocalDate#now()} 顶替</b> —— 协议口径是「下单时刻生效的那一版」，
     *                     用「今天」会让补录的历史单据随日历漂移。
     *                     为 {@code null}（单据日期缺失）时按既有逻辑回退。
     */
    public void createPayableOnReceipt(Long receiptId, String receiptNo, String supplierId,
                                       String supplierName, BigDecimal amount, LocalDate businessDate) {
        // 防重复记账：同一收货单已产生应付则整体跳过（应付与凭证一并跳过）
        if (payableService.existsBySource("PURCHASE_RECEIPT", receiptId)) {
            log.warn("收货单已存在应付记录，跳过重复记账: receiptId={}, receiptNo={}", receiptId, receiptNo);
            return;
        }
        log.info("创建采购应付及凭证: receiptNo={}, supplierId={}, amount={}", receiptNo, supplierId, amount);

        // 1. Create payable via finance integration service
        BusinessAccountingRequest payableRequest = new BusinessAccountingRequest();
        payableRequest.setSourceType("PURCHASE_RECEIPT");
        payableRequest.setSourceId(receiptId);
        payableRequest.setSourceNo(receiptNo);
        payableRequest.setSupplierId(supplierId);
        payableRequest.setSupplierName(supplierName);
        payableRequest.setAmount(amount);
        // 结算口径的两个身份字段（缺其一，财务侧判不了跨/内租户 ⇒ 按现款现结兜底）
        payableRequest.setBusinessDate(businessDate);
        payableRequest.setCounterpartyTenantId(resolveCounterpartyTenantId(supplierId));
        payableRequest.setDueDate(fallbackDueDate(businessDate));
        payableRequest.setSummary("采购收货 - " + receiptNo);

        PayableDTO payableResult = businessAccountingService.createPayableFromBusiness(payableRequest);
        log.info("应付创建成功: payableId={}", payableResult.getId());

        // 2. Create voucher via finance integration service
        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("PURCHASE_RECEIPT");
        voucherRequest.setSourceId(receiptId);
        voucherRequest.setSourceNo(receiptNo);
        voucherRequest.setSupplierId(supplierId);
        voucherRequest.setSupplierName(supplierName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("采购入库凭证 - " + receiptNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. Inventory, Cr. AP
        BusinessAccountingRequest.AccountingRequestItem debitEntry = new BusinessAccountingRequest.AccountingRequestItem();
        debitEntry.setSummary("采购入库");
        debitEntry.setSubjectCode("1403");  // 原材料
        debitEntry.setDebitAmount(amount);
        debitEntry.setCreditAmount(BigDecimal.ZERO);

        BusinessAccountingRequest.AccountingRequestItem creditEntry = new BusinessAccountingRequest.AccountingRequestItem();
        creditEntry.setSummary("采购入库");
        creditEntry.setSubjectCode("2202");  // 应付账款
        creditEntry.setDebitAmount(BigDecimal.ZERO);
        creditEntry.setCreditAmount(amount);

        voucherRequest.setItems(List.of(debitEntry, creditEntry));

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        log.info("凭证创建成功: voucherNo={}", voucherResult.getVoucherNo());
    }

    /**
     * 回退到期日 = <b>现款现结</b>（协议账期不介入时使用的那一个）。
     *
     * <p>⚠️ 这里<b>曾经是"业务日期 + 30 天"</b>（平台硬编码的默认账期）。按
     * DOMAIN-MODEL §13.3 补充口径 3 / §7.7 附 的裁定，**无协议 / 协议到期 /
     * 未约定结算方式一律走"无协议无特定客户模式" = 现款现结**，到期日 = <b>业务日</b>。
     * 凭空给 30 天账期等于平台替双方定商业条款（㉜），故移除。</p>
     *
     * <p>单据日期缺失（{@code businessDate == null}）时退到"今天"—— 口径仍是当天结清，
     * 不再多加 30 天。</p>
     */
    private static LocalDate fallbackDueDate(LocalDate businessDate) {
        return businessDate != null ? businessDate : LocalDate.now();
    }

    /**
     * **提交前预检**：本笔采购能否确定结算口径 —— 协议约定「账期结算」却缺天数 / 方向时
     * 抛业务异常，让<b>单据提交失败</b>（DOMAIN-MODEL §13.3 铁律②）。
     *
     * <p>⚠️ 调用方必须在记账的 try/catch <b>之外</b>调用本方法：下面
     * {@link #createPayableOnReceipt} 那一段的语义是"记账失败不阻断单据"，
     * 会把这个拒单理由一并吞掉，等于没拒。</p>
     */
    public void assertSettlementResolvable(String supplierId, LocalDate businessDate) {
        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setSupplierId(supplierId);
        request.setBusinessDate(businessDate);
        request.setCounterpartyTenantId(resolveCounterpartyTenantId(supplierId));
        businessAccountingService.precheckPayableDueDate(request);
    }

    /**
     * 取供应商所属租户（{@code biz_party.tenant_id}），供财务侧判定「按哪一份跨租户协议执行」。
     *
     * <p>取不到就返回 {@code null}：财务侧据此按「身份不足」处理 —— 不查协议、到期日走
     * {@link #fallbackDueDate}（现款现结）。<b>绝不用本租户顶替</b>：那会去查一份根本不存在的协议，
     * 或者更糟 —— 读到别人的商业条款。</p>
     *
     * <p>⚠️ 同租户交易是常态，而且它<b>不是</b>「没查到协议」的降级情形：
     * 取到的租户 = 本租户时，财务侧走的是<b>另一条口径</b> ——
     * 读本租户自己的往来单位结算档案（{@code biz_party.settlement_type} 等），
     * 因为账期是租户<b>单方</b>给供应商的付款信用政策，不涉对方约定（§7.7 附）。
     * 所以这个值必须如实取、如实传，它决定走哪一层口径。</p>
     */
    private Long resolveCounterpartyTenantId(String supplierId) {
        Long partyId = parsePartyId(supplierId);
        if (partyId == null) {
            return null;
        }
        try {
            return counterpartyTenantMapper.selectTenantIdByPartyId(partyId);
        } catch (Exception e) {
            // 取数失败不许影响记账：降级成「身份不足」，到期日走回退（不阻断单据）
            log.warn("取供应商所属租户失败，本笔应付不消费协议账期、到期日按既有逻辑生成: supplierId={}, 原因={}",
                    supplierId, e.getMessage());
            return null;
        }
    }

    /**
     * 往来方 ID 字符串 → {@code biz_party.id}。
     *
     * <p>非数字（例如只有名字快照）时返回 {@code null} —— 此时无法定位主体，按「身份不足」处理。
     * <b>绝不用名字去猜主体</b>：同名不同主体会查到别人的账期。</p>
     */
    private static Long parsePartyId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (!s.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
