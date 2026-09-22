package cn.aiedge.erp.sale.service.integration;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.ReceivableService;
import cn.aiedge.erp.sale.mapper.CounterpartyTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售模块记账集成服务
 * 直接调用财务模块的 {@link BusinessAccountingService} 创建应收与会计凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAccountingService {

    private final BusinessAccountingService businessAccountingService;
    private final ReceivableService receivableService;

    /** 取「客户所属租户」（协议账期需要两端租户；单据上只有客户 {@code biz_party.id}）。 */
    private final CounterpartyTenantMapper counterpartyTenantMapper;

    /**
     * 发货时创建应收和收入凭证
     * Dr. Accounts Receivable (应收账款)
     * Cr. Revenue (主营业务收入)
     *
     * @param shipmentId   发货单ID
     * @param shipmentNo   发货单编号
     * @param customerId   客户ID
     * @param customerName 客户名称
     * @param amount       金额
     * @param costAmount   <b>本单成本合计</b>（出库明细的 costAmount 求和，可为 null/0）。
     *                     大于 0 时凭证追加成本段「Dr. 主营业务成本(6401) / Cr. 库存商品(1405)」；
     *                     为 0 时省略成本段（与退货侧口径一致）。
     *                     <p>⚠️ 成本段必须与收入段写在<b>同一张凭证</b>里并一起过账。历史上出库的成本段
     *                     写在另一张永远停留在 draft 的本地凭证里 ⇒ 总账中销售成本从未结转。</p>
     * @param businessDate <b>单据的业务日期</b>（销售出库单的 {@code outboundDate} = 出库日期）。
     *                     它是「双方约定账期」的起算基准日：到期日 = 业务日期 + 约定天数。
     *                     <b>不许用 {@link LocalDate#now()} 顶替</b> —— 协议口径是「下单时刻生效的那一版」，
     *                     用「今天」会让补录的历史单据随日历漂移。
     *                     为 {@code null}（单据日期缺失）时按既有逻辑回退。
     *
     * <p>⚠️ 本方法被调用方包在 try/catch 里（"记账失败不阻断单据"），
     * 因此<b>不能</b>用它来实施"拒单"。协议账期缺项的拒单走
     * {@link #assertSettlementResolvable}，且必须放在那段 try/catch 之外。</p>
     */
    public void createReceivableOnShipment(Long shipmentId, String shipmentNo, String customerId,
                                           String customerName, BigDecimal amount, BigDecimal costAmount,
                                           LocalDate businessDate) {
        // 防重复记账：同一发货单已产生应收则整体跳过（应收与凭证一并跳过）
        if (receivableService.existsBySource("SALE_SHIPMENT", shipmentId)) {
            log.warn("发货单已存在应收记录，跳过重复记账: shipmentId={}, shipmentNo={}", shipmentId, shipmentNo);
            return;
        }
        log.info("创建销售应收及凭证: shipmentNo={}, customerId={}, amount={}", shipmentNo, customerId, amount);

        // 1. Create receivable via finance integration service
        BusinessAccountingRequest receivableRequest = new BusinessAccountingRequest();
        receivableRequest.setSourceType("SALE_SHIPMENT");
        receivableRequest.setSourceId(shipmentId);
        receivableRequest.setSourceNo(shipmentNo);
        receivableRequest.setCustomerId(customerId);
        receivableRequest.setCustomerName(customerName);
        receivableRequest.setAmount(amount);
        // 结算口径的两个身份字段（缺其一，财务侧判不了跨/内租户 ⇒ 按现款现结兜底）
        receivableRequest.setBusinessDate(businessDate);
        receivableRequest.setCounterpartyTenantId(resolveCounterpartyTenantId(customerId));
        receivableRequest.setDueDate(fallbackDueDate(businessDate));
        receivableRequest.setSummary("销售发货 - " + shipmentNo);

        ReceivableDTO receivableResult = businessAccountingService.createReceivableFromBusiness(receivableRequest);
        log.info("应收创建成功: receivableId={}", receivableResult.getId());

        // 2. Create voucher via finance integration service
        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("SALE_SHIPMENT");
        voucherRequest.setSourceId(shipmentId);
        voucherRequest.setSourceNo(shipmentNo);
        voucherRequest.setCustomerId(customerId);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("销售出库凭证 - " + shipmentNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        // Accounting entries: Dr. AR / Cr. Revenue（+ 成本段）
        // 往来科目行挂客户名作辅助核算项，供辅助核算余额表按客户归集（与退货侧同口径）。
        List<BusinessAccountingRequest.AccountingRequestItem> entries = new java.util.ArrayList<>();
        entries.add(buildEntry("销售出库", "1122", amount, BigDecimal.ZERO, customerName));      // 借 应收账款
        entries.add(buildEntry("确认收入", "6001", BigDecimal.ZERO, amount, null));              // 贷 主营业务收入

        BigDecimal cost = costAmount != null ? costAmount : BigDecimal.ZERO;
        if (cost.signum() > 0) {
            entries.add(buildEntry("结转销售成本", "6401", cost, BigDecimal.ZERO, null));       // 借 主营业务成本
            entries.add(buildEntry("结转销售成本", "1405", BigDecimal.ZERO, cost, null));       // 贷 库存商品
        }
        voucherRequest.setItems(entries);

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        log.info("凭证创建成功: voucherNo={}", voucherResult.getVoucherNo());
    }

    /**
     * 销售出库单取消时<b>作废</b>其应收。
     *
     * <p>取消的单据若把应收留在账上，客户对账单会凭空多出一笔欠款、账龄分析持续累计。
     * 与「核销」不同：核销表示钱收到了，作废表示这笔业务从未发生（见
     * {@link cn.aiedge.erp.finance.service.ReceivableService#cancelBySource}）。</p>
     *
     * <p>⚠️ 这是取消动作的<b>组成部分</b>，不是可选的记账增强 —— 调用方不要把它和
     * 「记账失败不阻断单据」的那段 try/catch 混在一起。</p>
     *
     * @return 实际作废条数；0 表示该单本就没有应收（未记账/已作废），属正常
     */
    public int cancelReceivableOnShipment(Long shipmentId, String reason) {
        int n = receivableService.cancelBySource("SALE_SHIPMENT", shipmentId, reason);
        if (n == 0) {
            log.info("取消出库单时未找到可作废的应收（本单未记账或已作废）: shipmentId={}", shipmentId);
        }
        return n;
    }

    /**
     * 销售出库单取消（已完成单）生成红字冲销凭证 —— 与退货侧的
     * {@link #createSaleReturnDocReverseVoucher} 完全对称。
     *
     * <p>收入段：Dr. 主营业务收入(6001) / Cr. 应收账款(1122)；</p>
     * <p>成本段：Dr. 库存商品(1405) / Cr. 主营业务成本(6401)（成本为 0 时省略）。</p>
     *
     * <p>走 {@code createVoucherFromBusiness} ⇒ 自动 create→audit→post。
     * 历史实现是本地 {@code voucherService.create} 写一张**永远停在 draft** 的红冲凭证
     * ⇒ 总账上被取消单据的收入仍然成立。</p>
     *
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleOutboundReverseVoucher(Long outboundId, String outboundNo, String customerId,
                                                   String customerName, BigDecimal amount, BigDecimal costAmount) {
        if (amount == null || amount.signum() <= 0) {
            log.warn("销售出库单金额为0，跳过红冲: outboundNo={}", outboundNo);
            return "";
        }
        log.info("创建销售出库单红冲凭证: outboundNo={}, customerId={}, amount={}, costAmount={}",
                outboundNo, customerId, amount, costAmount);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType("SALE_OUTBOUND_CANCEL");
        voucherRequest.setSourceId(outboundId);
        voucherRequest.setSourceNo(outboundNo);
        voucherRequest.setCustomerId(customerId);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary("取消出库红冲 - " + outboundNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> entries = new java.util.ArrayList<>();
        entries.add(buildEntry("取消出库冲减收入", "6001", amount, BigDecimal.ZERO, null));
        entries.add(buildEntry("取消出库冲减应收", "1122", BigDecimal.ZERO, amount, customerName));

        BigDecimal cost = costAmount != null ? costAmount : BigDecimal.ZERO;
        if (cost.signum() > 0) {
            entries.add(buildEntry("取消出库转回库存", "1405", cost, BigDecimal.ZERO, null));
            entries.add(buildEntry("取消出库转回成本", "6401", BigDecimal.ZERO, cost, null));
        }
        voucherRequest.setItems(entries);

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        String voucherNo = voucherResult.getVoucherNo() != null ? voucherResult.getVoucherNo() : "";
        log.info("销售出库单红冲凭证创建成功: outboundNo={}, voucherNo={}", outboundNo, voucherNo);
        return voucherNo;
    }

    /**
     * 回退到期日 = <b>现款现结</b>（协议账期不介入时使用的那一个）。
     *
     * <p>⚠️ 这里<b>曾经是"业务日期 + 30 天"</b>（平台硬编码的默认账期）。按
     * DOMAIN-MODEL §13.3 补充口径 3 / §7.7 附 的裁定，**无协议 / 协议到期 /
     * 未约定结算方式一律走"无协议无特定客户模式" = 现款现结**（钱货两清、零信用敞口），
     * 到期日 = <b>业务日</b>。凭空给 30 天账期等于平台替双方定商业条款（㉜），故移除。</p>
     *
     * <p>单据日期缺失（{@code businessDate == null}）时退到"今天"—— 口径仍是当天结清，
     * 不再多加 30 天。</p>
     */
    private static LocalDate fallbackDueDate(LocalDate businessDate) {
        return businessDate != null ? businessDate : LocalDate.now();
    }

    /**
     * **提交前预检**：本笔销售能否确定结算口径 —— 协议约定「账期结算」却缺天数 / 方向时
     * 抛业务异常，让<b>单据提交失败</b>（DOMAIN-MODEL §13.3 铁律②）。
     *
     * <p>⚠️ 调用方必须在记账的 try/catch <b>之外</b>调用本方法：下面
     * {@link #createReceivableOnShipment} 那一段的语义是"记账失败不阻断单据"，
     * 会把这个拒单理由一并吞掉，等于没拒。</p>
     */
    public void assertSettlementResolvable(String customerId, LocalDate businessDate) {
        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setCustomerId(customerId);
        request.setBusinessDate(businessDate);
        request.setCounterpartyTenantId(resolveCounterpartyTenantId(customerId));
        businessAccountingService.precheckReceivableDueDate(request);
    }

    /**
     * 取客户所属租户（{@code biz_party.tenant_id}），供财务侧判定「按哪一份跨租户协议执行」。
     *
     * <p>取不到就返回 {@code null}：财务侧据此按「身份不足」处理 —— 不查协议、到期日走
     * {@link #fallbackDueDate}（现款现结）。<b>绝不用本租户顶替</b>：那会去查一份根本不存在的协议，
     * 或者更糟 —— 读到别人的商业条款。</p>
     *
     * <p>⚠️ 同租户交易是常态，而且它<b>不是</b>「没查到协议」的降级情形：
     * 取到的租户 = 本租户时，财务侧走的是<b>另一条口径</b> ——
     * 读本租户自己的往来单位结算档案（{@code biz_party.settlement_type} 等），
     * 因为账期是租户<b>单方</b>给客户的信用政策，不涉对方约定（§7.7 附）。
     * 所以这个值必须如实取、如实传，它决定走哪一层口径。</p>
     */
    private Long resolveCounterpartyTenantId(String customerId) {
        Long partyId = parsePartyId(customerId);
        if (partyId == null) {
            return null;
        }
        try {
            return counterpartyTenantMapper.selectTenantIdByPartyId(partyId);
        } catch (Exception e) {
            // 取数失败不许影响记账：降级成「身份不足」，到期日走回退（不阻断单据）
            log.warn("取客户所属租户失败，本笔应收不消费协议账期、到期日按既有逻辑生成: customerId={}, 原因={}",
                    customerId, e.getMessage());
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

    /**
     * 销售退货申请审核通过时创建红字冲销凭证
     * <p>收入段：Dr. 主营业务收入(6001) / Cr. 应收账款(1122)——冲回已确认的收入与应收；</p>
     * <p>成本段：Dr. 库存商品(1405) / Cr. 主营业务成本(6401)——退货入库冲回已结转成本（成本为 0 时省略）。</p>
     *
     * @param returnId     退货申请单ID
     * @param returnNo     退货申请单编号
     * @param customerId   客户ID
     * @param customerName 客户名称
     * @param amount       退货金额（含税，冲减收入与应收）
     * @param costAmount   退货成本金额（参考成本合计，可为 null/0）
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnVoucher(Long returnId, String returnNo, Long customerId, String customerName,
                                          BigDecimal amount, BigDecimal costAmount) {
        return postSaleReturnVoucher(returnId, returnNo, customerId, customerName, amount, costAmount, false);
    }

    /**
     * 销售退货申请取消（已记账后）生成反向冲回凭证
     * <p>收入段：Dr. 应收账款(1122) / Cr. 主营业务收入(6001)；</p>
     * <p>成本段：Dr. 主营业务成本(6401) / Cr. 库存商品(1405)。</p>
     *
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnReverseVoucher(Long returnId, String returnNo, Long customerId, String customerName,
                                                 BigDecimal amount, BigDecimal costAmount) {
        return postSaleReturnVoucher(returnId, returnNo, customerId, customerName, amount, costAmount, true);
    }

    /**
     * 销售退货单（实际退货入库单）审核通过时生成红字冲销凭证
     * <p>口径与退货申请一致：收入段 Dr. 主营业务收入(6001) / Cr. 应收账款(1122)；
     * 成本段 Dr. 库存商品(1405) / Cr. 主营业务成本(6401)（成本为 0 时省略）。</p>
     *
     * @param returnDocId  退货单ID
     * @param returnDocNo  退货单编号
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnDocVoucher(Long returnDocId, String returnDocNo, Long customerId, String customerName,
                                             BigDecimal amount, BigDecimal costAmount) {
        return postReturnDocVoucher(returnDocId, returnDocNo, customerId, customerName, amount, costAmount, false);
    }

    /**
     * 销售退货单取消（已记账后）生成反向冲回凭证
     *
     * @return 凭证编号；未生成时返回空串
     */
    public String createSaleReturnDocReverseVoucher(Long returnDocId, String returnDocNo, Long customerId,
                                                    String customerName, BigDecimal amount, BigDecimal costAmount) {
        return postReturnDocVoucher(returnDocId, returnDocNo, customerId, customerName, amount, costAmount, true);
    }

    private String postReturnDocVoucher(Long returnDocId, String returnDocNo, Long customerId, String customerName,
                                        BigDecimal amount, BigDecimal costAmount, boolean reversed) {
        if (amount == null || amount.signum() <= 0) {
            log.warn("销售退货单金额为0，跳过记账: returnDocNo={}", returnDocNo);
            return "";
        }
        String sourceType = reversed ? "SALE_RETURN_DOC_CANCEL" : "SALE_RETURN_DOC";
        String prefix = reversed ? "销售退货单取消冲回" : "销售退货单冲销";
        log.info("创建销售退货单凭证: returnDocNo={}, customerId={}, amount={}, costAmount={}, reversed={}",
                returnDocNo, customerId, amount, costAmount, reversed);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType(sourceType);
        voucherRequest.setSourceId(returnDocId);
        voucherRequest.setSourceNo(returnDocNo);
        voucherRequest.setCustomerId(customerId != null ? String.valueOf(customerId) : null);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary(prefix + " - " + returnDocNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> entries = new java.util.ArrayList<>();
        entries.add(buildEntry(reversed ? "销售退货单取消回补收入" : "销售退货单冲减收入", "6001",
                reversed ? BigDecimal.ZERO : amount, reversed ? amount : BigDecimal.ZERO, null));
        entries.add(buildEntry(reversed ? "销售退货单取消回补应收" : "销售退货单冲减应收", "1122",
                reversed ? amount : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : amount, customerName));

        BigDecimal cost = costAmount != null ? costAmount : BigDecimal.ZERO;
        if (cost.signum() > 0) {
            entries.add(buildEntry(reversed ? "销售退货单取消转回成本" : "退货入库", "1405",
                    reversed ? BigDecimal.ZERO : cost, reversed ? cost : BigDecimal.ZERO, null));
            entries.add(buildEntry(reversed ? "销售退货单取消转回库存" : "冲回主营业务成本", "6401",
                    reversed ? cost : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : cost, null));
        }
        voucherRequest.setItems(entries);

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        String voucherNo = voucherResult.getVoucherNo() != null ? voucherResult.getVoucherNo() : "";
        log.info("销售退货单凭证创建成功: returnDocNo={}, voucherNo={}", returnDocNo, voucherNo);
        return voucherNo;
    }

    private String postSaleReturnVoucher(Long returnId, String returnNo, Long customerId, String customerName,
                                         BigDecimal amount, BigDecimal costAmount, boolean reversed) {
        if (amount == null || amount.signum() <= 0) {
            log.warn("退货申请单金额为0，跳过记账: returnNo={}", returnNo);
            return "";
        }
        String sourceType = reversed ? "SALE_RETURN_APPLY_CANCEL" : "SALE_RETURN_APPLY";
        String prefix = reversed ? "销售退货取消冲回" : "销售退货冲销";
        log.info("创建销售退货凭证: returnNo={}, customerId={}, amount={}, costAmount={}, reversed={}",
                returnNo, customerId, amount, costAmount, reversed);

        BusinessAccountingRequest voucherRequest = new BusinessAccountingRequest();
        voucherRequest.setSourceType(sourceType);
        voucherRequest.setSourceId(returnId);
        voucherRequest.setSourceNo(returnNo);
        voucherRequest.setCustomerId(customerId != null ? String.valueOf(customerId) : null);
        voucherRequest.setCustomerName(customerName);
        voucherRequest.setAmount(amount);
        voucherRequest.setSummary(prefix + " - " + returnNo);
        voucherRequest.setVoucherDate(LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> entries = new java.util.ArrayList<>();

        // 收入段：正向冲减收入与应收；反向回补（往来科目行挂客户核算项，供辅助核算余额表归集）
        entries.add(buildEntry(reversed ? "销售退货取消回补收入" : "销售退货冲减收入", "6001",
                reversed ? BigDecimal.ZERO : amount, reversed ? amount : BigDecimal.ZERO, null));
        entries.add(buildEntry(reversed ? "销售退货取消回补应收" : "销售退货冲减应收", "1122",
                reversed ? amount : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : amount, customerName));

        // 成本段：正向退货入库冲回已结转成本；反向退回出库恢复成本
        BigDecimal cost = costAmount != null ? costAmount : BigDecimal.ZERO;
        if (cost.signum() > 0) {
            entries.add(buildEntry(reversed ? "销售退货取消转回成本" : "退货入库", "1405",
                    reversed ? BigDecimal.ZERO : cost, reversed ? cost : BigDecimal.ZERO, null));
            entries.add(buildEntry(reversed ? "销售退货取消转回库存" : "冲回主营业务成本", "6401",
                    reversed ? cost : BigDecimal.ZERO, reversed ? BigDecimal.ZERO : cost, null));
        }

        voucherRequest.setItems(entries);

        VoucherDTO voucherResult = businessAccountingService.createVoucherFromBusiness(voucherRequest);
        String voucherNo = voucherResult.getVoucherNo() != null ? voucherResult.getVoucherNo() : "";
        log.info("销售退货凭证创建成功: returnNo={}, voucherNo={}", returnNo, voucherNo);
        return voucherNo;
    }

    private BusinessAccountingRequest.AccountingRequestItem buildEntry(String summary, String subjectCode,
                                                                      BigDecimal debit, BigDecimal credit,
                                                                      String auxUnit) {
        BusinessAccountingRequest.AccountingRequestItem entry = new BusinessAccountingRequest.AccountingRequestItem();
        entry.setSummary(summary);
        entry.setSubjectCode(subjectCode);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);
        entry.setAuxUnit(auxUnit);
        return entry;
    }
}
