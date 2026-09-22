package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.base.credit.CreditTermQuery;
import cn.aiedge.base.credit.CreditTermResult;
import cn.aiedge.base.credit.TradeCreditTermProvider;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PartySettlementProfile;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.dto.VoucherItemDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.PartySettlementProfileMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.finance.service.ReceivableService;
import cn.aiedge.erp.finance.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 业财集成网关Service实现类
 * 接收来自业务系统（采购、销售、费用等）的记账请求，统一转换为财务凭证
 *
 * <h3>结算口径 → 应收/应付到期日（DOMAIN-MODEL §7.7 附「三层结算口径」的唯一消费方）</h3>
 * 本类是本仓<b>唯一</b>把「结算口径」落到到期日上的地方（应收/应付的写入口都在这里）。
 * 优先级链<b>收敛在本类一处</b>（§7.7 附裁定），不许在调用方各写一遍：
 *
 * <table border="1">
 *   <tr><th>#</th><th>场景</th><th>用哪层口径</th><th>到期日</th></tr>
 *   <tr><td>1</td><td><b>跨租户</b> + 有协议且约定了账期</td>
 *       <td>协议</td><td>业务日 + 约定天数</td></tr>
 *   <tr><td>2</td><td><b>跨租户</b> + 协议约定的结算方式是现款 / 滚结</td>
 *       <td>协议</td><td>业务日（无账期）</td></tr>
 *   <tr><td>3</td><td><b>跨租户</b> + 无协议 / 协议到期 / 未约定结算方式</td>
 *       <td>无协议无特定客户模式</td><td>业务日（<b>现款现结</b>，低风险缺省）</td></tr>
 *   <tr><td>4</td><td><b>租户内</b>（客户就是我自己的客户）</td>
 *       <td>{@code biz_party} 结算档案</td><td>业务日 + 档案天数（现结 ⇒ 业务日）</td></tr>
 *   <tr><td>5</td><td>档案也没设 / 身份不足</td><td>—</td><td>业务日（<b>现款现结</b>）</td></tr>
 * </table>
 *
 * <p>账期口径从 {@link TradeCreditTermProvider} 取（契约接口在 core-base，实现在协议模块）。
 * 本类<b>不</b>依赖协议模块，用 {@link ObjectProvider} 容忍"实现不存在"：
 * {@code getIfAvailable() == null} 时行为<b>与接入之前逐字一致</b>。</p>
 *
 * <h3>⚠️ 唯一的"拦单"情形：约定了「账期结算」却缺账期天数 / 方向</h3>
 * 这时 {@link CreditTermResult#isUndeclared()} 为真，本类<b>抛业务异常拒绝这笔单据提交</b>
 * （§13.3 铁律②：账期天数是<b>条件必填</b>，缺 ⇒ 提交不了）。
 * 其余三态一律不拦单：本笔无账期是<b>正常结论</b>、无适用协议走现款现结、协议取数异常降级放行。
 *
 * <p>⚠️ 但"抛异常"只在<b>提交处</b>才算数：{@link #createReceivableFromBusiness} 的调用方
 * 是 catch-and-continue（"记账失败不阻断单据"），会把拒单理由一并吞掉。
 * 因此单据流程必须在记账的 try/catch <b>之外</b>调用 {@link #precheckReceivableDueDate} /
 * {@link #precheckPayableDueDate} —— 见这两个方法的注释。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessAccountingServiceImpl implements BusinessAccountingService {

    private static final String USAGE_AR = "生成应收到期日";
    private static final String USAGE_AP = "生成应付到期日";

    private final VoucherService voucherService;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final AccountSubjectMapper accountSubjectMapper;

    /**
     * 协议结算口径提供方；实现可能<b>不存在</b>（协议模块未部署 / 被裁剪），
     * 故用 {@link ObjectProvider} 可选注入 —— 缺实现时行为与接入前逐字一致。
     */
    private final ObjectProvider<TradeCreditTermProvider> tradeCreditTermProvider;

    /** 取"本端租户"（判定跨租户 / 租户内，以及读本租户自己的往来档案，都要它）。 */
    private final SecurityContext securityContext;

    /** 租户内交易的结算口径来源：{@code biz_party} 上的结算档案。 */
    private final PartySettlementProfileMapper partySettlementProfileMapper;

    // 业财集成入口统一使用 REQUIRES_NEW：记账在独立事务中提交/回滚，
    // 既不因记账失败污染调用方事务（调用方均为 catch-and-continue），
    // 也保持与原 HTTP 内调时代一致的"记账独立提交"语义
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public VoucherDTO createVoucherFromBusiness(BusinessAccountingRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BusinessException.badRequest("记账明细项不能为空");
        }

        LocalDate voucherDate = request.getVoucherDate() != null ? request.getVoucherDate() : LocalDate.now();
        Integer fiscalYear = voucherDate.getYear();
        Integer fiscalPeriod = voucherDate.getMonthValue();

        List<VoucherItemDTO> items = new ArrayList<>();
        for (BusinessAccountingRequest.AccountingRequestItem reqItem : request.getItems()) {
            AccountSubject subject = accountSubjectMapper.findBySubjectCode(reqItem.getSubjectCode())
                    .orElseThrow(() -> BusinessException.notFound("科目编码不存在: " + reqItem.getSubjectCode()));

            VoucherItemDTO item = new VoucherItemDTO();
            item.setSummary(reqItem.getSummary() != null ? reqItem.getSummary() : request.getSummary());
            item.setSubjectId(subject.getId());
            item.setSubjectCode(subject.getSubjectCode());
            item.setSubjectName(subject.getSubjectName());
            item.setDebitAmount(reqItem.getDebitAmount() != null ? reqItem.getDebitAmount() : BigDecimal.ZERO);
            item.setCreditAmount(reqItem.getCreditAmount() != null ? reqItem.getCreditAmount() : BigDecimal.ZERO);
            item.setSourceType(request.getSourceType());
            item.setSourceId(request.getSourceId());
            item.setSourceNo(request.getSourceNo());
            // 核算项：往来科目行带往来单位，费用科目行带部门/职员，供辅助核算余额表按核算项归集
            item.setAuxUnit(reqItem.getAuxUnit());
            item.setAuxDept(reqItem.getAuxDept());
            item.setAuxStaff(reqItem.getAuxStaff());
            items.add(item);
        }

        VoucherDTO voucherDTO = new VoucherDTO();
        voucherDTO.setVoucherDate(voucherDate);
        voucherDTO.setFiscalYear(fiscalYear);
        voucherDTO.setFiscalPeriod(fiscalPeriod);
        // 摘要与来源单号必须在此显式传递：VoucherServiceImpl#create 读的是 dto.getSummary()/dto.getSourceNo()，
        // 此前只 setRemark 导致 finance_voucher.summary/source_no 全空，凭证无法反查业务单据（2026-09-20 修复）
        voucherDTO.setSummary(request.getSummary());
        voucherDTO.setSourceNo(request.getSourceNo());
        voucherDTO.setRemark(request.getSummary());
        voucherDTO.setAttachments(0);
        voucherDTO.setItems(items);

        BigDecimal totalDebit = items.stream()
                .map(i -> i.getDebitAmount() != null ? i.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = items.stream()
                .map(i -> i.getCreditAmount() != null ? i.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        voucherDTO.setTotalDebit(totalDebit);
        voucherDTO.setTotalCredit(totalCredit);

        VoucherDTO created = voucherService.create(voucherDTO);

        try {
            String systemUser = "system";
            VoucherDTO audited = voucherService.audit(created.getId(), systemUser);
            VoucherDTO posted = voucherService.post(audited.getId(), systemUser);
            log.info("业财集成自动过账完成: sourceType={}, sourceId={}, voucherNo={}",
                    request.getSourceType(), request.getSourceId(), posted.getVoucherNo());
            return posted;
        } catch (Exception e) {
            log.warn("业财集成自动过账失败，保留草稿状态: sourceType={}, sourceId={}, voucherId={}, error={}",
                    request.getSourceType(), request.getSourceId(), created.getId(), e.getMessage());
            return created;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ReceivableDTO createReceivableFromBusiness(BusinessAccountingRequest request) {
        ReceivableDTO dto = new ReceivableDTO();
        dto.setSourceType(request.getSourceType());
        dto.setSourceId(request.getSourceId());
        dto.setSourceNo(request.getSourceNo());
        dto.setCustomerId(request.getCustomerId());
        dto.setCustomerName(request.getCustomerName());
        dto.setTotalAmount(request.getAmount());
        dto.setRemainingAmount(request.getAmount());
        dto.setPaidAmount(BigDecimal.ZERO);
        dto.setDueDate(resolveDueDate(request, true));
        dto.setInvoiceDate(request.getVoucherDate());
        dto.setStatus("normal");

        ReceivableDTO created = receivableService.create(dto);
        log.info("业财集成创建应收账款: sourceType={}, sourceId={}",
                request.getSourceType(), request.getSourceId());
        return created;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public PayableDTO createPayableFromBusiness(BusinessAccountingRequest request) {
        PayableDTO dto = new PayableDTO();
        dto.setSourceType(request.getSourceType());
        dto.setSourceId(request.getSourceId());
        dto.setSourceNo(request.getSourceNo());
        dto.setSupplierId(request.getSupplierId());
        dto.setSupplierName(request.getSupplierName());
        dto.setTotalAmount(request.getAmount());
        dto.setRemainingAmount(request.getAmount());
        dto.setPaidAmount(BigDecimal.ZERO);
        dto.setDueDate(resolveDueDate(request, false));
        dto.setInvoiceDate(request.getVoucherDate());
        dto.setStatus("normal");

        PayableDTO created = payableService.create(dto);
        log.info("业财集成创建应付账款: sourceType={}, sourceId={}",
                request.getSourceType(), request.getSourceId());
        return created;
    }

    @Override
    public LocalDate precheckReceivableDueDate(BusinessAccountingRequest request) {
        return resolveDueDate(request, true);
    }

    @Override
    public LocalDate precheckPayableDueDate(BusinessAccountingRequest request) {
        return resolveDueDate(request, false);
    }

    // ══════════════════════ 结算口径 → 到期日（三层优先级链的唯一收敛处） ══════════════════════

    /**
     * 生成本笔应收/应付的到期日 —— <b>三层结算口径的完整优先级链</b>（§7.7 附裁定）。
     *
     * <ol>
     *   <li><b>身份不足</b>（拿不到业务日期 / 对方租户 / 本端租户）⇒ 现款现结（业务日）。
     *       宁可落到最低风险那一端，也不编一个主体或租户去查别人家的条款。</li>
     *   <li><b>跨租户</b> ⇒ 协议口径（{@link #resolveCrossTenantDueDate}）。</li>
     *   <li><b>租户内</b> ⇒ 往来单位结算档案（{@link #resolveIntraTenantDueDate}）。</li>
     * </ol>
     *
     * <p>⚠️ 唯一的"拦单"出口在跨租户分支：协议约定了「账期结算」却缺天数 / 方向时
     * <b>抛业务异常</b>（§13.3 铁律②）。其余一律不拦单。</p>
     *
     * @param receivableSide true = 应收（对方是客户/买方），false = 应付（对方是供应商/卖方）
     * @return 到期日；{@code null} 仅当调用方自己也没给、且业务日期缺失（无从推算）时
     */
    private LocalDate resolveDueDate(BusinessAccountingRequest request, boolean receivableSide) {
        // 调用方带来的既有值：只在"业务日期缺失、什么口径都算不出来"时兜底。
        LocalDate fallback = request.getDueDate();

        TradeCreditTermProvider provider = tradeCreditTermProvider.getIfAvailable();
        if (provider == null) {
            // 接口无实现（协议模块未部署）⇒ 与接入前逐字一致，连日志都不多加。
            return fallback;
        }

        LocalDate businessDate = request.getBusinessDate();
        Long selfTenantId = securityContext.getCurrentTenantId();
        Long counterpartyTenantId = request.getCounterpartyTenantId();
        if (businessDate == null || selfTenantId == null || counterpartyTenantId == null) {
            // 身份不足 ⇒ 判不了跨/内租户，也读不到档案主体 ⇒ 现款现结（低风险缺省）。
            log.debug("未提供业务日期 / 对方主体所属租户（或无会话租户），本笔按现款现结处理: sourceType={}, sourceId={}",
                    request.getSourceType(), request.getSourceId());
            return cashOnSpot(businessDate, fallback);
        }

        if (selfTenantId.equals(counterpartyTenantId)) {
            // 租户内交易：账期是本租户**单方**给客户的信用政策，不涉对方约定 ⇒ 用自己档案上的口径。
            return resolveIntraTenantDueDate(request, receivableSide, businessDate, fallback);
        }
        return resolveCrossTenantDueDate(request, receivableSide, provider, businessDate, fallback);
    }

    /**
     * **跨租户** ⇒ 协议口径（{@link CreditTermResult} 四态各自处置）。
     *
     * <table border="1">
     *   <tr><th>协议结果</th><th>到期日</th><th>单据</th></tr>
     *   <tr><td>{@code AGREED}（约定了账期结算 + 天数 + 方向一致）</td>
     *       <td><b>业务日期 + 约定天数</b></td><td>照常生成</td></tr>
     *   <tr><td>{@code NO_CREDIT_TERM}（约定的是现款现货 / 滚结）</td>
     *       <td><b>业务日期</b>（无账期，当天结清）</td><td>照常生成 —— 这是<b>正常结论</b></td></tr>
     *   <tr><td>{@code NO_AGREEMENT}（无协议 / 到期 / 未约定结算方式）</td>
     *       <td><b>业务日期</b>（无协议无特定客户模式 = 现款现结）</td><td>照常生成</td></tr>
     *   <tr><td>{@code UNDECLARED}（账期结算缺天数 / 方向）</td>
     *       <td>—</td><td><b>拒绝提交</b>（抛业务异常）</td></tr>
     * </table>
     *
     * <p>协议侧取数失败（数据异常、提供方返回空）一律<b>降级成现款现结</b>并留痕，
     * <b>不</b>阻断单据：拒单只留给"双方明明选了账期结算却没写全"这一种情形。</p>
     */
    private LocalDate resolveCrossTenantDueDate(BusinessAccountingRequest request, boolean receivableSide,
                                                TradeCreditTermProvider provider,
                                                LocalDate businessDate, LocalDate fallback) {
        CreditTermQuery query = buildCreditTermQuery(request, receivableSide, businessDate);
        if (query == null) {
            // 缺本端/对方主体 ID（例如只有名字快照）⇒ 查不到协议，按现款现结兜底。
            log.debug("往来主体 ID 不可用（只有名字快照？），本笔不消费协议结算口径，按现款现结处理: sourceType={}, sourceId={}",
                    request.getSourceType(), request.getSourceId());
            return cashOnSpot(businessDate, fallback);
        }

        CreditTermResult result;
        try {
            result = provider.resolve(query);
        } catch (Exception e) {
            // 协议侧取数异常（例如同一对主体并存两份生效协议）不许阻断开单：降级成现款现结并留痕
            log.warn("取协议结算口径失败，本笔按现款现结兜底（不阻断单据）: 业务日期={}, 对方租户={}, 原因={}",
                    businessDate, request.getCounterpartyTenantId(), e.getMessage());
            return cashOnSpot(businessDate, fallback);
        }
        if (result == null) {
            log.warn("协议结算口径提供方返回空结果，本笔按现款现结兜底: 业务日期={}", businessDate);
            return cashOnSpot(businessDate, fallback);
        }

        if (result.isUndeclared()) {
            // §13.3 铁律②：账期天数是**条件必填**，缺 ⇒ 本笔单据提交不了（不是留空挂人工）。
            throw BusinessException.badRequest("本笔单据不能提交 —— 协议约定的结算方式是「账期结算」，"
                    + "但账期天数或账期方向没有约定完整：" + result.getReason()
                    + "。请双方在协议里补全该必填项后再开单");
        }
        if (result.isNoAgreement()) {
            log.debug("本笔交易无适用协议（无协议 / 已到期 / 未约定结算方式）⇒ 走无协议无特定客户模式，现款现结: {}",
                    result.getReason());
            return cashOnSpot(businessDate, fallback);
        }
        if (result.isNoCreditTerm()) {
            LocalDate dueDate = result.dueDateFrom(businessDate);
            log.info("本笔按协议约定的结算方式无账期（结算方式={}）⇒ 到期日 = 业务日 {}（协议 {}, 第 {} 版）",
                    result.getSettlementType(), dueDate, result.getAgreementId(), result.getVersionNo());
            return dueDate;
        }

        LocalDate dueDate = result.dueDateFrom(businessDate);
        log.info("按协议账期生成本笔到期日: 业务日期 {} + 约定 {} 天 = {}（协议 {}, 第 {} 版）",
                businessDate, result.getCreditDays(), dueDate,
                result.getAgreementId(), result.getVersionNo());
        return dueDate;
    }

    /**
     * **租户内** ⇒ 往来单位结算档案口径（§7.7 附：账期是租户单方给客户的信用政策，
     * 不涉及对方约定，故与第四条原则「单方承诺不传导」不冲突 —— 它本来就不传导）。
     *
     * <p>档案口径的取值（{@code biz_party}，历史两值编码，见 {@link PartySettlementProfile}）：</p>
     * <ul>
     *   <li>{@code settlement_type = 0}（现结）⇒ <b>无账期</b>，到期日 = 业务日；</li>
     *   <li>{@code settlement_type ≠ 0}（有账期）⇒ 应收看 {@code credit_days}、应付看 {@code payment_days}，
     *       到期日 = 业务日 + 该天数；</li>
     *   <li>档案行取不到 / 写了有账期却没填有效天数 ⇒ <b>现款现结</b>兜底。
     *       这里<b>不编</b>一个默认天数：平台给默认值等于替租户定他的信用政策（㉜）。</li>
     * </ul>
     *
     * <p>⚠️ 档案读失败一律降级放行，不阻断单据 —— 档案是主数据，不能因为它取不到就开不了单。</p>
     */
    private LocalDate resolveIntraTenantDueDate(BusinessAccountingRequest request, boolean receivableSide,
                                                LocalDate businessDate, LocalDate fallback) {
        Long partyId = parsePartyId(receivableSide ? request.getCustomerId() : request.getSupplierId());
        Long selfTenantId = securityContext.getCurrentTenantId();
        if (partyId == null || selfTenantId == null) {
            return cashOnSpot(businessDate, fallback);
        }

        PartySettlementProfile profile;
        try {
            profile = partySettlementProfileMapper.selectByPartyId(partyId, selfTenantId);
        } catch (Exception e) {
            log.warn("读往来单位结算档案失败，本笔按现款现结兜底（不阻断单据）: partyId={}, 原因={}",
                    partyId, e.getMessage());
            return cashOnSpot(businessDate, fallback);
        }
        if (profile == null) {
            log.debug("往来单位无结算档案，本笔按现款现结处理: partyId={}", partyId);
            return cashOnSpot(businessDate, fallback);
        }

        Integer settlementType = profile.getSettlementType();
        if (settlementType == null || settlementType == 0) {
            // 0 = 现结：档案本身就是"钱货两清、无账期"，与现款现结同一口径
            log.debug("往来单位档案为「现结」，本笔无账期 ⇒ 到期日 = 业务日: partyId={}", partyId);
            return cashOnSpot(businessDate, fallback);
        }

        Integer days = receivableSide ? profile.getCreditDays() : profile.getPaymentDays();
        if (days == null || days <= 0) {
            log.warn("往来单位档案写的是「非现结」但没填有效天数，本笔按现款现结处理（不编默认天数）: "
                    + "partyId={}, 方向={}, creditDays={}, paymentDays={}",
                    partyId, receivableSide ? "应收" : "应付", profile.getCreditDays(), profile.getPaymentDays());
            return cashOnSpot(businessDate, fallback);
        }

        LocalDate dueDate = businessDate.plusDays(days);
        log.info("按往来单位结算档案生成本笔到期日: 业务日期 {} + 档案 {} 天 = {}（partyId={}, 方向={}）",
                businessDate, days, dueDate, partyId, receivableSide ? "应收" : "应付");
        return dueDate;
    }

    /**
     * **现款现结**的到期日：钱货两清 ⇒ 到期日 = <b>业务日</b>（当天结清、不产生账龄）。
     *
     * <p>这是"无协议无特定客户模式"的默认口径（§13.3 ⑤）——
     * <b>不是</b>历史上的"今天 + 30 天账期"。业务日期缺失（单据日期没传）时，
     * 退回调用方带来的既有值，不凭空造一个日期。</p>
     */
    private static LocalDate cashOnSpot(LocalDate businessDate, LocalDate fallback) {
        return businessDate != null ? businessDate : fallback;
    }

    /**
     * 用<b>本笔账已经拿到的事实</b>拼协议查询入参；事实不足时返回 {@code null}（= 查不了协议）。
     *
     * <p>本端租户取当前会话租户；对方主体就是请求里的 {@code customerId} / {@code supplierId}
     * （本仓这两个字段存的就是 {@code biz_party.id}）；对方主体所属租户由调用方给出。</p>
     *
     * @param businessDate 已由调用方校验非空，直接复用，不再重复判空
     */
    private CreditTermQuery buildCreditTermQuery(BusinessAccountingRequest request, boolean receivableSide,
                                                LocalDate businessDate) {
        Long selfTenantId = securityContext.getCurrentTenantId();
        if (selfTenantId == null) {
            // 无会话租户（内部任务、无登录上下文）⇒ 无法定住租户对，不查协议
            return null;
        }
        if (receivableSide) {
            Long customerPartyId = parsePartyId(request.getCustomerId());
            if (customerPartyId == null) {
                return null;
            }
            return CreditTermQuery.of(selfTenantId, null,
                    request.getCounterpartyTenantId(), customerPartyId,
                    businessDate, USAGE_AR);
        }
        Long supplierPartyId = parsePartyId(request.getSupplierId());
        if (supplierPartyId == null) {
            return null;
        }
        return CreditTermQuery.of(request.getCounterpartyTenantId(), supplierPartyId,
                selfTenantId, null,
                businessDate, USAGE_AP);
    }

    /**
     * 往来方 ID → {@code biz_party.id}。
     *
     * <p>非数字（例如只有名字快照）时返回 {@code null} —— 此时无法定位主体，
     * 本笔账按"无适用协议"处理。<b>绝不用名字去猜主体</b>：同名不同主体会查到别人的账期。</p>
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
