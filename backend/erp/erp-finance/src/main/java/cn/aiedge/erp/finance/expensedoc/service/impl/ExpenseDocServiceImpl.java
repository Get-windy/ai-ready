package cn.aiedge.erp.finance.expensedoc.service.impl;

import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocItemVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocSaveDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocVO;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseDoc;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseItem;
import cn.aiedge.erp.finance.expensedoc.mapper.ExpenseDocMapper;
import cn.aiedge.erp.finance.expensedoc.mapper.ExpenseItemMapper;
import cn.aiedge.erp.budget.dto.BudgetWritebackItem;
import cn.aiedge.erp.budget.dto.BudgetWritebackRequest;
import cn.aiedge.erp.budget.dto.BudgetWritebackResult;
import cn.aiedge.erp.budget.service.BudgetWritebackService;
import cn.aiedge.erp.finance.expensedoc.service.ExpenseDocService;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.FinanceAccountService;
import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 费用单 Service实现
 *
 * P0 红线：费用记账必须经会计凭证（KJPZ-）入总账/明细账，严禁绕过凭证直改费用/往来余额；
 *         费用项明细必须有映射科目（借方），付款账户（贷方）映射资金类科目。
 * P1 守恒：本单金额 = Σ费用项金额 = Σ付款金额。
 * P1 预算：费用记账后按费用科目回写《预算执行》（已执行↑，超支转预警不阻断记账）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseDocServiceImpl extends ServiceImpl<ExpenseDocMapper, ExpenseDoc> implements ExpenseDocService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_POSTED = 1;
    private static final int STATUS_CANCELLED = 2;
    /** 审批状态：1-审批中 3-审批驳回（P1 门控：审批中/已驳回不可记账） */
    private static final int APPROVAL_APPROVING = 1;
    private static final int APPROVAL_REJECTED = 3;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    /** 费用科目兜底：管理费用 */
    private static final String SUBJECT_FEE = "6602";
    /** 资金类科目兜底：银行存款 */
    private static final String SUBJECT_BANK = "1002";

    private final ExpenseItemMapper expenseItemMapper;
    private final FinanceAccountService financeAccountService;
    private final CapitalFlowService capitalFlowService;
    private final BusinessAccountingService businessAccountingService;
    private final BudgetWritebackService budgetWritebackService;

    @Override
    public Page<ExpenseDoc> pageQuery(ExpenseDocQuery query) {
        LambdaQueryWrapper<ExpenseDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseDoc::getDeleted, 0);
        if (query == null) {
            query = new ExpenseDocQuery();
        }
        if (query.getDocNo() != null && !query.getDocNo().isEmpty()) {
            wrapper.like(ExpenseDoc::getDocNo, query.getDocNo());
        }
        if (query.getPartnerName() != null && !query.getPartnerName().isEmpty()) {
            wrapper.like(ExpenseDoc::getPartnerName, query.getPartnerName());
        }
        if (query.getHandlerName() != null && !query.getHandlerName().isEmpty()) {
            wrapper.like(ExpenseDoc::getHandlerName, query.getHandlerName());
        }
        if (query.getDeptName() != null && !query.getDeptName().isEmpty()) {
            wrapper.like(ExpenseDoc::getDeptName, query.getDeptName());
        }
        if (query.getCreatorName() != null && !query.getCreatorName().isEmpty()) {
            wrapper.like(ExpenseDoc::getCreatorName, query.getCreatorName());
        }
        if (query.getBookkeeperName() != null && !query.getBookkeeperName().isEmpty()) {
            wrapper.like(ExpenseDoc::getBookkeeperName, query.getBookkeeperName());
        }
        if (query.getPayAccountName() != null && !query.getPayAccountName().isEmpty()) {
            wrapper.like(ExpenseDoc::getPayAccountName, query.getPayAccountName());
        }
        if (query.getRemark() != null && !query.getRemark().isEmpty()) {
            wrapper.like(ExpenseDoc::getRemark, query.getRemark());
        }
        if (query.getSummary() != null && !query.getSummary().isEmpty()) {
            wrapper.like(ExpenseDoc::getSummary, query.getSummary());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ExpenseDoc::getStatus, query.getStatus());
        }
        if (query.getExpenseType() != null) {
            wrapper.eq(ExpenseDoc::getExpenseType, query.getExpenseType());
        }
        if (query.getApprovalStatus() != null) {
            wrapper.eq(ExpenseDoc::getApprovalStatus, query.getApprovalStatus());
        }
        if (query.getDateStart() != null) {
            wrapper.ge(ExpenseDoc::getDocDate, query.getDateStart());
        }
        if (query.getDateEnd() != null) {
            wrapper.le(ExpenseDoc::getDocDate, query.getDateEnd());
        }
        if (query.getShowRed() != null && !query.getShowRed()) {
            wrapper.and(w -> w.isNull(ExpenseDoc::getRedFlag).or().eq(ExpenseDoc::getRedFlag, 0));
        }
        wrapper.orderByDesc(ExpenseDoc::getDocDate).orderByDesc(ExpenseDoc::getDocNo);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public Page<ExpenseDocItemVO> pageDetail(ExpenseDocQuery query) {
        if (query == null) {
            query = new ExpenseDocQuery();
        }
        Page<ExpenseDocItemVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.pageDetail(page, query);
    }

    @Override
    public String generateDocNo() {
        String prefix = "YBFYD";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<ExpenseDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(ExpenseDoc::getDocNo, prefix + "-" + dateStr)
                .eq(ExpenseDoc::getDeleted, 0)
                .orderByDesc(ExpenseDoc::getDocNo)
                .last("LIMIT 1");
        ExpenseDoc last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getDocNo();
            String tail = lastNo.substring(lastNo.lastIndexOf('-') + 1);
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + "-" + dateStr + "-" + String.format("%03d", seq);
    }

    @Override
    public ExpenseDocVO getDetail(Long id) {
        ExpenseDoc doc = getById(id);
        if (doc == null) {
            throw new cn.aiedge.common.exception.BusinessException("费用单不存在");
        }
        ExpenseDocVO vo = new ExpenseDocVO();
        BeanUtils.copyProperties(doc, vo);
        vo.setItems(expenseItemMapper.selectList(new LambdaQueryWrapper<ExpenseItem>()
                .eq(ExpenseItem::getExpenseDocId, id)
                .eq(ExpenseItem::getDeleted, 0)
                .orderByAsc(ExpenseItem::getLineNo)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDoc saveDraft(ExpenseDocSaveDTO dto) {
        ExpenseDoc doc;
        if (dto.getId() != null) {
            doc = getById(dto.getId());
            if (doc == null) {
                throw new cn.aiedge.common.exception.BusinessException("费用单不存在");
            }
            if (doc.getStatus() == STATUS_POSTED) {
                throw new cn.aiedge.common.exception.BusinessException("已记账的费用单不能修改");
            }
            if (approvalStatusOf(doc) == APPROVAL_APPROVING) {
                throw new cn.aiedge.common.exception.BusinessException("审批中的费用单不能修改，请先驳回或撤回审批");
            }
        } else {
            doc = new ExpenseDoc();
            doc.setDocNo(dto.getDocNo() != null && !dto.getDocNo().isEmpty() ? dto.getDocNo() : generateDocNo());
        }
        applyHeader(doc, dto);
        doc.setStatus(STATUS_DRAFT);
        doc.setTenantId(doc.getTenantId() != null ? doc.getTenantId() : 1L);
        doc.setPrintCount(doc.getPrintCount() != null ? doc.getPrintCount() : 0);
        // 落守恒字段（P1）：totalAmount = Σ费用项金额 = Σ付款金额
        BigDecimal itemTotal = sumItemAmounts(dto.getItems());
        BigDecimal payTotal = sumPayAmounts(dto);
        if (itemTotal.compareTo(payTotal) != 0) {
            throw new cn.aiedge.common.exception.BusinessException(
                    "金额不平衡：费用项合计(" + itemTotal + ")应等于付款合计(" + payTotal + ")");
        }
        doc.setTotalAmount(itemTotal);
        if (dto.getId() != null) {
            updateById(doc);
        } else {
            save(doc);
        }
        replaceItems(doc.getId(), dto.getItems());
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDoc update(ExpenseDocSaveDTO dto) {
        return saveDraft(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseDoc confirm(Long id, Long operatorId, String operatorName) {
        ExpenseDoc doc = getById(id);
        if (doc == null) {
            throw new cn.aiedge.common.exception.BusinessException("费用单不存在");
        }
        if (doc.getStatus() == STATUS_POSTED) {
            return doc; // 幂等
        }
        if (doc.getStatus() == STATUS_CANCELLED) {
            throw new cn.aiedge.common.exception.BusinessException("已取消的费用单不能记账");
        }
        // P1 审批门控：审批中/已驳回的费用单禁止记账（与费用审批页共用一套状态）
        if (approvalStatusOf(doc) == APPROVAL_APPROVING) {
            throw new cn.aiedge.common.exception.BusinessException("费用单正在审批中，审批通过后方可记账");
        }
        if (approvalStatusOf(doc) == APPROVAL_REJECTED) {
            throw new cn.aiedge.common.exception.BusinessException("费用单已驳回，请修改后重新提交审批再记账");
        }
        // 重算金额守恒（P1）：费用项合计 = 付款合计 = 本单金额
        BigDecimal itemTotal = sumItems(id);
        BigDecimal payTotal = sumPayFromDoc(doc);
        if (itemTotal.compareTo(payTotal) != 0) {
            throw new cn.aiedge.common.exception.BusinessException(
                    "金额不平衡：费用项合计(" + itemTotal + ")应等于付款合计(" + payTotal + ")");
        }
        doc.setTotalAmount(itemTotal);
        doc.setBookkeeperId(operatorId);
        doc.setBookkeeperName(operatorName != null ? operatorName : doc.getCreatorName());
        doc.setBookkeepingTime(LocalDateTime.now());
        doc.setStatus(STATUS_POSTED);
        updateById(doc);

        // 动账户余额（P0：凭证为主手段，余额为资金账户展现）
        moveAccountBalances(doc);

        // 记资金流水
        recordExpenseFlows(doc);

        // 记账生成凭证（KJPZ-，P0 红线：必须经凭证）
        try {
            createVoucher(doc);
        } catch (Exception e) {
            log.error("费用记账创建凭证失败: docNo={}, error={}", doc.getDocNo(), e.getMessage(), e);
            throw new cn.aiedge.common.exception.BusinessException("记账失败：生成会计凭证异常:" + e.getMessage());
        }

        // P1 执行回写：按费用科目把已记账支出计入《预算执行》的「已执行」
        writeBackBudget(doc);
        return doc;
    }

    /**
     * P1 执行回写：费用记账后回写《预算执行》。
     *
     * <p>按「会计年度 + 部门 + 费用科目编码」匹配已审批/执行中的预算科目并计入已执行。
     * 超支不阻断记账（单据已完成记账），转为超支预警在《预算执行》呈现；
     * 未匹配到预算的科目记 warn 日志，便于后续接通预算。</p>
     */
    private void writeBackBudget(ExpenseDoc doc) {
        List<ExpenseItem> items = expenseItemMapper.selectList(new LambdaQueryWrapper<ExpenseItem>()
                .eq(ExpenseItem::getExpenseDocId, doc.getId())
                .eq(ExpenseItem::getDeleted, 0)
                .orderByAsc(ExpenseItem::getLineNo));
        if (items.isEmpty()) {
            return;
        }

        LocalDate execDate = doc.getDocDate() != null ? doc.getDocDate() : LocalDate.now();
        BudgetWritebackRequest request = new BudgetWritebackRequest();
        request.setSourceType("expense");
        request.setSourceNo(doc.getDocNo());
        request.setSourceId(doc.getId());
        request.setExecDate(execDate);
        request.setFiscalYear(execDate.getYear());
        request.setDepartmentId(doc.getDeptId() != null ? String.valueOf(doc.getDeptId()) : null);

        List<BudgetWritebackItem> lines = new ArrayList<>();
        for (ExpenseItem item : items) {
            BudgetWritebackItem line = new BudgetWritebackItem();
            line.setSubjectCode(item.getSubjectCode());
            line.setSubjectName(item.getSubjectName());
            line.setAmount(item.getAmount());
            lines.add(line);
        }
        request.setItems(lines);

        BudgetWritebackResult result = budgetWritebackService.consume(request);
        if (result.getSkippedCount() > 0 || result.getOverBudgetCount() > 0) {
            log.warn("费用记账预算回写：单号={} 匹配={} 未匹配={} 超支={} 说明={}",
                    doc.getDocNo(), result.getMatchedCount(), result.getSkippedCount(),
                    result.getOverBudgetCount(), result.getMessages());
        } else {
            log.info("费用记账预算回写：单号={} 匹配={} 金额={}",
                    doc.getDocNo(), result.getMatchedCount(), result.getTotalConsumed());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ExpenseDoc doc = getById(id);
        if (doc == null) {
            throw new cn.aiedge.common.exception.BusinessException("费用单不存在");
        }
        if (doc.getStatus() == STATUS_POSTED) {
            throw new cn.aiedge.common.exception.BusinessException("已记账的费用单不能取消");
        }
        doc.setStatus(STATUS_CANCELLED);
        updateById(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        ExpenseDoc doc = getById(id);
        if (doc == null) {
            throw new cn.aiedge.common.exception.BusinessException("费用单不存在");
        }
        if (doc.getStatus() == STATUS_POSTED) {
            throw new cn.aiedge.common.exception.BusinessException("已记账的费用单不能删除");
        }
        removeById(id);
    }

    // ── 内部方法 ──

    private void applyHeader(ExpenseDoc doc, ExpenseDocSaveDTO dto) {
        doc.setDocDate(dto.getDocDate() != null ? dto.getDocDate() : LocalDate.now());
        doc.setExpenseType(dto.getExpenseType() != null ? dto.getExpenseType() : 0);
        doc.setPartnerId(dto.getPartnerId());
        doc.setPartnerCode(dto.getPartnerCode());
        doc.setPartnerName(dto.getPartnerName());
        doc.setHandlerId(dto.getHandlerId());
        doc.setHandlerName(dto.getHandlerName());
        doc.setDeptId(dto.getDeptId());
        doc.setDeptName(dto.getDeptName());
        doc.setPayAccountId(dto.getPayAccountId());
        doc.setPayAccountName(dto.getPayAccountName());
        doc.setPaySubjectCode(dto.getPayAccountId() != null ? SUBJECT_BANK : null);
        doc.setPayAmount(nvl(dto.getPayAmount()));
        doc.setPayAccount2Id(dto.getPayAccount2Id());
        doc.setPayAccount2Name(dto.getPayAccount2Name());
        doc.setPaySubjectCode2(dto.getPayAccount2Id() != null ? SUBJECT_BANK : null);
        doc.setPayAmount2(nvl(dto.getPayAmount2()));
        doc.setPayAccount3Id(dto.getPayAccount3Id());
        doc.setPayAccount3Name(dto.getPayAccount3Name());
        doc.setPaySubjectCode3(dto.getPayAccount3Id() != null ? SUBJECT_BANK : null);
        doc.setPayAmount3(nvl(dto.getPayAmount3()));
        doc.setPayAccount4Id(dto.getPayAccount4Id());
        doc.setPayAccount4Name(dto.getPayAccount4Name());
        doc.setPaySubjectCode4(dto.getPayAccount4Id() != null ? SUBJECT_BANK : null);
        doc.setPayAmount4(nvl(dto.getPayAmount4()));
        doc.setCreatorName(dto.getCreatorName());
        doc.setSummary(dto.getSummary());
        doc.setRemark(dto.getRemark());
    }

    private void replaceItems(Long expenseDocId, List<ExpenseItem> items) {
        expenseItemMapper.delete(new LambdaQueryWrapper<ExpenseItem>().eq(ExpenseItem::getExpenseDocId, expenseDocId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int line = 1;
        for (ExpenseItem item : items) {
            item.setId(null);
            item.setExpenseDocId(expenseDocId);
            item.setLineNo(line++);
            item.setTenantId(1L);
            item.setSubjectCode(item.getSubjectCode() != null && !item.getSubjectCode().isEmpty()
                    ? item.getSubjectCode() : SUBJECT_FEE);
            expenseItemMapper.insert(item);
        }
    }

    private BigDecimal sumItemAmounts(List<ExpenseItem> items) {
        if (items == null) {
            return ZERO;
        }
        return items.stream().map(i -> nvl(i.getAmount())).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumPayAmounts(ExpenseDocSaveDTO dto) {
        return nvl(dto.getPayAmount()).add(nvl(dto.getPayAmount2()))
                .add(nvl(dto.getPayAmount3())).add(nvl(dto.getPayAmount4()));
    }

    private BigDecimal sumItems(Long expenseDocId) {
        List<ExpenseItem> items = expenseItemMapper.selectList(new LambdaQueryWrapper<ExpenseItem>()
                .eq(ExpenseItem::getExpenseDocId, expenseDocId)
                .eq(ExpenseItem::getDeleted, 0));
        return items.stream().map(i -> nvl(i.getAmount())).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumPayFromDoc(ExpenseDoc doc) {
        return nvl(doc.getPayAmount()).add(nvl(doc.getPayAmount2()))
                .add(nvl(doc.getPayAmount3())).add(nvl(doc.getPayAmount4()));
    }

    /** 动付款账户余额：付款账户1-4 余额各扣相应金额 */
    private void moveAccountBalances(ExpenseDoc doc) {
        decreaseBalance(doc.getPayAccountId(), doc.getPayAmount());
        decreaseBalance(doc.getPayAccount2Id(), doc.getPayAmount2());
        decreaseBalance(doc.getPayAccount3Id(), doc.getPayAmount3());
        decreaseBalance(doc.getPayAccount4Id(), doc.getPayAmount4());
    }

    private void decreaseBalance(Long accountId, BigDecimal amount) {
        if (accountId == null || nvl(amount).compareTo(ZERO) <= 0) {
            return;
        }
        financeAccountService.updateAccountBalance(accountId, nvl(amount).negate());
    }

    /** 记资金流水：每条付款账户一条 OUT */
    private void recordExpenseFlows(ExpenseDoc doc) {
        createFlow(doc, doc.getPayAccountId(), doc.getPayAccountName(), doc.getPayAmount(), 1);
        createFlow(doc, doc.getPayAccount2Id(), doc.getPayAccount2Name(), doc.getPayAmount2(), 2);
        createFlow(doc, doc.getPayAccount3Id(), doc.getPayAccount3Name(), doc.getPayAmount3(), 3);
        createFlow(doc, doc.getPayAccount4Id(), doc.getPayAccount4Name(), doc.getPayAmount4(), 4);
    }

    private void createFlow(ExpenseDoc doc, Long accountId, String accountName, BigDecimal amount, int idx) {
        if (accountId == null || nvl(amount).compareTo(ZERO) <= 0) {
            return;
        }
        CapitalFlow flow = new CapitalFlow();
        flow.setFlowType("EXPENSE_DOC");
        flow.setDirection("OUT");
        flow.setRefId(doc.getId());
        flow.setRefNo(doc.getDocNo());
        flow.setRefType("ExpenseDoc");
        flow.setAmount(nvl(amount));
        flow.setBusinessType("expense_doc_pay");
        flow.setOccurDate(LocalDateTime.now());
        flow.setRemark("费用支付" + idx + "(" + (accountName != null ? accountName : "") + ") - " + doc.getDocNo());
        capitalFlowService.createFlow(flow);
    }

    /**
     * 记账生成凭证（KJPZ-）：
     *  Dr 各费用项科目(费用明细金额, 兜底6602) / Cr 各付款账户科目(付款金额, 兜底1002)
     *  借贷平衡：Σ借 = Σ贷 = 本单金额。
     */
    private void createVoucher(ExpenseDoc doc) {
        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setSourceType("EXPENSE_DOC");
        request.setSourceId(doc.getId());
        request.setSourceNo(doc.getDocNo());
        request.setAmount(nvl(doc.getTotalAmount()));
        request.setSummary("费用单 - " + doc.getDocNo());
        request.setVoucherDate(doc.getDocDate() != null ? doc.getDocDate() : LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> items = new ArrayList<>();
        BigDecimal debitTotal = ZERO;

        // 借方：各费用项 → 费用科目
        List<ExpenseItem> itemList = expenseItemMapper.selectList(new LambdaQueryWrapper<ExpenseItem>()
                .eq(ExpenseItem::getExpenseDocId, doc.getId())
                .eq(ExpenseItem::getDeleted, 0)
                .orderByAsc(ExpenseItem::getLineNo));
        for (ExpenseItem item : itemList) {
            BigDecimal amt = nvl(item.getAmount());
            if (amt.compareTo(ZERO) <= 0) {
                continue;
            }
            BusinessAccountingRequest.AccountingRequestItem debit = new BusinessAccountingRequest.AccountingRequestItem();
            debit.setSummary(item.getExpenseName() != null ? item.getExpenseName() : "费用项");
            debit.setSubjectCode(item.getSubjectCode() != null && !item.getSubjectCode().isEmpty()
                    ? item.getSubjectCode() : SUBJECT_FEE);
            debit.setDebitAmount(amt);
            debit.setCreditAmount(ZERO);
            // 核算项：费用借方按单据部门/经手人归集，供辅助核算余额表按部门、职员维度分析
            debit.setAuxDept(doc.getDeptName());
            debit.setAuxStaff(doc.getHandlerName());
            items.add(debit);
            debitTotal = debitTotal.add(amt);
        }

        // 贷方：各付款账户 → 资金类科目
        BigDecimal creditTotal = ZERO;
        creditTotal = creditTotal.add(addCredit(items, doc.getPayAccountName(), doc.getPaySubjectCode(), doc.getPayAmount()));
        creditTotal = creditTotal.add(addCredit(items, doc.getPayAccount2Name(), doc.getPaySubjectCode2(), doc.getPayAmount2()));
        creditTotal = creditTotal.add(addCredit(items, doc.getPayAccount3Name(), doc.getPaySubjectCode3(), doc.getPayAmount3()));
        creditTotal = creditTotal.add(addCredit(items, doc.getPayAccount4Name(), doc.getPaySubjectCode4(), doc.getPayAmount4()));

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new cn.aiedge.common.exception.BusinessException(
                    "凭证借贷不平衡：借方(" + debitTotal + ")应等于贷方(" + creditTotal + ")");
        }

        request.setItems(items);
        businessAccountingService.createVoucherFromBusiness(request);
    }

    private BigDecimal addCredit(List<BusinessAccountingRequest.AccountingRequestItem> items,
                                 String accountName, String subjectCode, BigDecimal amount) {
        BigDecimal amt = nvl(amount);
        if (amt.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        BusinessAccountingRequest.AccountingRequestItem credit = new BusinessAccountingRequest.AccountingRequestItem();
        credit.setSummary(accountName != null ? accountName : "付款账户");
        credit.setSubjectCode(subjectCode != null && !subjectCode.isEmpty() ? subjectCode : SUBJECT_BANK);
        credit.setDebitAmount(ZERO);
        credit.setCreditAmount(amt);
        items.add(credit);
        return amt;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    /** 审批状态：0-未提交 1-审批中 2-审批通过 3-审批驳回 */
    private int approvalStatusOf(ExpenseDoc doc) {
        return doc.getApprovalStatus() != null ? doc.getApprovalStatus() : 0;
    }
}
