package cn.aiedge.erp.finance.cashtransfer.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferItemVO;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferQuery;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferSaveDTO;
import cn.aiedge.erp.finance.cashtransfer.dto.CashTransferVO;
import cn.aiedge.erp.finance.cashtransfer.entity.CashTransfer;
import cn.aiedge.erp.finance.cashtransfer.entity.CashTransferItem;
import cn.aiedge.erp.finance.cashtransfer.mapper.CashTransferItemMapper;
import cn.aiedge.erp.finance.cashtransfer.mapper.CashTransferMapper;
import cn.aiedge.erp.finance.cashtransfer.service.CashTransferService;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.mapper.FinanceAccountMapper;
import cn.aiedge.erp.finance.model.entity.FinanceAccount;
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
 * 提存（提存现金转账）Service实现
 *
 * P0 红线：资金账户间划转，记账必须经会计凭证（KJPZ-），严禁绕过凭证直改账户余额；
 *         不涉及往来单位（不产生应收/应付）。
 * P1 守恒：本单金额 = Σ转入金额 + 手续费 = 转出金额。
 * P1 手续费：按承担账户/科目归集，计入费用（管理费用 6602）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashTransferServiceImpl extends ServiceImpl<CashTransferMapper, CashTransfer> implements CashTransferService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_POSTED = 1;
    private static final int STATUS_CANCELLED = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String SUBJECT_CASH = "1001";    // 库存现金
    private static final String SUBJECT_BANK = "1002";    // 银行存款
    private static final String SUBJECT_FEE = "6602";     // 管理费用（手续费承担科目）

    private final CashTransferItemMapper cashTransferItemMapper;
    private final FinanceAccountService financeAccountService;
    private final FinanceAccountMapper financeAccountMapper;
    private final CapitalFlowService capitalFlowService;
    private final BusinessAccountingService businessAccountingService;

    @Override
    public Page<CashTransfer> pageQuery(CashTransferQuery query) {
        LambdaQueryWrapper<CashTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CashTransfer::getDeleted, 0);
        if (query == null) {
            query = new CashTransferQuery();
        }
        if (query.getDocNo() != null && !query.getDocNo().isEmpty()) {
            wrapper.like(CashTransfer::getDocNo, query.getDocNo());
        }
        if (query.getHandlerName() != null && !query.getHandlerName().isEmpty()) {
            wrapper.like(CashTransfer::getHandlerName, query.getHandlerName());
        }
        if (query.getDeptName() != null && !query.getDeptName().isEmpty()) {
            wrapper.like(CashTransfer::getDeptName, query.getDeptName());
        }
        if (query.getCreatorName() != null && !query.getCreatorName().isEmpty()) {
            wrapper.like(CashTransfer::getCreatorName, query.getCreatorName());
        }
        if (query.getBookkeeperName() != null && !query.getBookkeeperName().isEmpty()) {
            wrapper.like(CashTransfer::getBookkeeperName, query.getBookkeeperName());
        }
        if (query.getFromAccountName() != null && !query.getFromAccountName().isEmpty()) {
            wrapper.like(CashTransfer::getFromAccountName, query.getFromAccountName());
        }
        if (query.getRemark() != null && !query.getRemark().isEmpty()) {
            wrapper.like(CashTransfer::getRemark, query.getRemark());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CashTransfer::getStatus, query.getStatus());
        }
        if (query.getDateStart() != null) {
            wrapper.ge(CashTransfer::getDocDate, query.getDateStart());
        }
        if (query.getDateEnd() != null) {
            wrapper.le(CashTransfer::getDocDate, query.getDateEnd());
        }
        if (query.getShowRed() != null && !query.getShowRed()) {
            wrapper.and(w -> w.isNull(CashTransfer::getRedFlag).or().eq(CashTransfer::getRedFlag, 0));
        }
        wrapper.orderByDesc(CashTransfer::getDocDate).orderByDesc(CashTransfer::getDocNo);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public Page<CashTransferItemVO> pageDetail(CashTransferQuery query) {
        if (query == null) {
            query = new CashTransferQuery();
        }
        Page<CashTransferItemVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.pageDetail(page, query);
    }

    @Override
    public String generateDocNo() {
        String prefix = "YHZKD";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<CashTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CashTransfer::getDocNo, prefix + "-" + dateStr)
                .eq(CashTransfer::getDeleted, 0)
                .orderByDesc(CashTransfer::getDocNo)
                .last("LIMIT 1");
        CashTransfer last = getOne(wrapper);
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
    public CashTransferVO getDetail(Long id) {
        CashTransfer transfer = getById(id);
        if (transfer == null) {
            throw BusinessException.notFound("提存单不存在");
        }
        CashTransferVO vo = new CashTransferVO();
        BeanUtils.copyProperties(transfer, vo);
        vo.setItems(cashTransferItemMapper.selectList(new LambdaQueryWrapper<CashTransferItem>()
                .eq(CashTransferItem::getTransferId, id)
                .eq(CashTransferItem::getDeleted, 0)
                .orderByAsc(CashTransferItem::getLineNo)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashTransfer saveDraft(CashTransferSaveDTO dto) {
        CashTransfer transfer;
        if (dto.getId() != null) {
            transfer = getById(dto.getId());
            if (transfer == null) {
                throw BusinessException.notFound("提存单不存在");
            }
            if (transfer.getStatus() == STATUS_POSTED) {
                throw BusinessException.badRequest("已记账的提存单不能修改");
            }
        } else {
            transfer = new CashTransfer();
            transfer.setDocNo(dto.getDocNo() != null && !dto.getDocNo().isEmpty() ? dto.getDocNo() : generateDocNo());
        }
        applyHeader(transfer, dto);
        transfer.setStatus(STATUS_DRAFT);
        transfer.setTenantId(transfer.getTenantId() != null ? transfer.getTenantId() : 1L);
        transfer.setPrintCount(transfer.getPrintCount() != null ? transfer.getPrintCount() : 0);
        // 落守恒字段（P1）：toAmount=Σ转入，totalAmount=转出金额
        BigDecimal toAmount = nvl(dto.getFromAmount());
        if (dto.getItems() != null) {
            toAmount = dto.getItems().stream().map(i -> nvl(i.getAmount())).reduce(ZERO, BigDecimal::add);
        }
        transfer.setToAmount(toAmount);
        transfer.setTotalAmount(nvl(transfer.getFromAmount()));
        if (dto.getId() != null) {
            updateById(transfer);
        } else {
            save(transfer);
        }
        replaceItems(transfer.getId(), dto.getItems());
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashTransfer update(CashTransferSaveDTO dto) {
        return saveDraft(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashTransfer confirm(Long id, Long operatorId, String operatorName) {
        CashTransfer transfer = getById(id);
        if (transfer == null) {
            throw BusinessException.notFound("提存单不存在");
        }
        if (transfer.getStatus() == STATUS_POSTED) {
            return transfer; // 幂等
        }
        if (transfer.getStatus() == STATUS_CANCELLED) {
            throw BusinessException.badRequest("已取消的提存单不能记账");
        }
        // 重算金额守恒：转账金额 = Σ转入 + 手续费（P1）
        BigDecimal toAmount = sumItems(transfer.getId());
        BigDecimal fee = nvl(transfer.getFee());
        BigDecimal fromAmount = nvl(transfer.getFromAmount());
        if (fromAmount.compareTo(toAmount.add(fee)) != 0) {
            throw BusinessException.badRequest("金额不平衡：转出金额(" + fromAmount + ")应等于转入合计(" + toAmount + ")+手续费(" + fee + ")");
        }
        transfer.setToAmount(toAmount);
        transfer.setTotalAmount(fromAmount);
        // 记账前校验账户余额充足（转出账户）
        validateSufficientBalance(transfer);
        transfer.setBookkeeperId(operatorId);
        transfer.setBookkeeperName(operatorName != null ? operatorName : transfer.getCreatorName());
        transfer.setBookkeepingTime(LocalDateTime.now());
        transfer.setStatus(STATUS_POSTED);
        updateById(transfer);

        // 动账户余额（P0：凭证为主手段，余额为资金账户展现）
        moveAccountBalances(transfer);

        // 记资金流水（双边 OUT/IN）
        recordCashTransferFlows(transfer);

        // 记账生成凭证（KJPZ-，P0 红线：必须经凭证）
        try {
            createVoucher(transfer);
        } catch (Exception e) {
            log.error("提存记账创建凭证失败: docNo={}, error={}", transfer.getDocNo(), e.getMessage(), e);
            throw BusinessException.badRequest("记账失败：生成会计凭证异常:" + e.getMessage());
        }
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        CashTransfer transfer = getById(id);
        if (transfer == null) {
            throw BusinessException.notFound("提存单不存在");
        }
        if (transfer.getStatus() == STATUS_POSTED) {
            throw BusinessException.badRequest("已记账的提存单不能取消");
        }
        transfer.setStatus(STATUS_CANCELLED);
        updateById(transfer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        CashTransfer transfer = getById(id);
        if (transfer == null) {
            throw BusinessException.notFound("提存单不存在");
        }
        if (transfer.getStatus() == STATUS_POSTED) {
            throw BusinessException.badRequest("已记账的提存单不能删除");
        }
        removeById(id);
    }

    // ── 内部方法 ──

    private void applyHeader(CashTransfer transfer, CashTransferSaveDTO dto) {
        transfer.setDocDate(dto.getDocDate() != null ? dto.getDocDate() : LocalDate.now());
        transfer.setFromAccountId(dto.getFromAccountId());
        transfer.setFromAccountName(dto.getFromAccountName());
        transfer.setFromAccountType(dto.getFromAccountType());
        transfer.setFromSubjectCode(dto.getFromSubjectCode() != null ? dto.getFromSubjectCode() : subjectCodeOf(dto.getFromAccountType()));
        transfer.setFromAmount(nvl(dto.getFromAmount()));
        transfer.setFee(nvl(dto.getFee()));
        transfer.setHandlerId(dto.getHandlerId());
        transfer.setHandlerName(dto.getHandlerName());
        transfer.setDeptId(dto.getDeptId());
        transfer.setDeptName(dto.getDeptName());
        transfer.setCreatorName(dto.getCreatorName());
        transfer.setSummary(dto.getSummary());
        transfer.setRemark(dto.getRemark());
    }

    private void replaceItems(Long transferId, List<CashTransferItem> items) {
        cashTransferItemMapper.delete(new LambdaQueryWrapper<CashTransferItem>().eq(CashTransferItem::getTransferId, transferId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int line = 1;
        for (CashTransferItem item : items) {
            item.setId(null);
            item.setTransferId(transferId);
            item.setLineNo(line++);
            item.setTenantId(1L);
            item.setToSubjectCode(item.getToSubjectCode() != null ? item.getToSubjectCode() : subjectCodeOf(item.getToAccountType()));
            cashTransferItemMapper.insert(item);
        }
    }

    private BigDecimal sumItems(Long transferId) {
        List<CashTransferItem> items = cashTransferItemMapper.selectList(new LambdaQueryWrapper<CashTransferItem>()
                .eq(CashTransferItem::getTransferId, transferId)
                .eq(CashTransferItem::getDeleted, 0));
        return items.stream().map(i -> nvl(i.getAmount())).reduce(ZERO, BigDecimal::add);
    }

    /** 按账户类型映射资金类科目编码：1银行→1002，2现金→1001，3内部/4外部→1002 */
    private String subjectCodeOf(Integer accountType) {
        if (accountType == null) {
            return SUBJECT_CASH;
        }
        return accountType == 2 ? SUBJECT_CASH : SUBJECT_BANK;
    }

    /** 动账户余额：转出账户减转出金额，转入账户加转入金额 */
    private void moveAccountBalances(CashTransfer transfer) {
        if (transfer.getFromAccountId() != null) {
            boolean ok = financeAccountService.updateAccountBalance(transfer.getFromAccountId(), nvl(transfer.getFromAmount()).negate());
            if (!ok) {
                throw BusinessException.badRequest("转出账户不存在或余额更新失败");
            }
        }
        List<CashTransferItem> items = cashTransferItemMapper.selectList(new LambdaQueryWrapper<CashTransferItem>()
                .eq(CashTransferItem::getTransferId, transfer.getId())
                .eq(CashTransferItem::getDeleted, 0));
        for (CashTransferItem item : items) {
            if (item.getToAccountId() != null && nvl(item.getAmount()).compareTo(ZERO) > 0) {
                financeAccountService.updateAccountBalance(item.getToAccountId(), nvl(item.getAmount()));
            }
        }
    }

    /** 记资金流水：转出一条 OUT，转入账户每条 IN（仿 recordOffsetFlow 双边模式） */
    private void recordCashTransferFlows(CashTransfer transfer) {
        CapitalFlow outFlow = new CapitalFlow();
        outFlow.setFlowType("CASH_TRANSFER");
        outFlow.setDirection("OUT");
        outFlow.setRefId(transfer.getId());
        outFlow.setRefNo(transfer.getDocNo());
        outFlow.setRefType("CashTransfer");
        outFlow.setAmount(nvl(transfer.getFromAmount()));
        outFlow.setBusinessType("cash_transfer_out");
        outFlow.setOccurDate(LocalDateTime.now());
        outFlow.setRemark("提存转出 - " + transfer.getDocNo());
        capitalFlowService.createFlow(outFlow);

        List<CashTransferItem> items = cashTransferItemMapper.selectList(new LambdaQueryWrapper<CashTransferItem>()
                .eq(CashTransferItem::getTransferId, transfer.getId())
                .eq(CashTransferItem::getDeleted, 0));
        for (CashTransferItem item : items) {
            if (nvl(item.getAmount()).compareTo(ZERO) <= 0) {
                continue;
            }
            CapitalFlow inFlow = new CapitalFlow();
            inFlow.setFlowType("CASH_TRANSFER");
            inFlow.setDirection("IN");
            inFlow.setRefId(transfer.getId());
            inFlow.setRefNo(transfer.getDocNo());
            inFlow.setRefType("CashTransfer");
            inFlow.setAmount(nvl(item.getAmount()));
            inFlow.setBusinessType("cash_transfer_in");
            inFlow.setOccurDate(LocalDateTime.now());
            inFlow.setRemark("提存转入(" + item.getToAccountName() + ") - " + transfer.getDocNo());
            capitalFlowService.createFlow(inFlow);
        }
    }

    /**
     * 记账生成凭证（KJPZ-）：
     *  Dr 转入账户科目(各明细金额) / Dr 手续费(6602=手续费) / Cr 转出账户科目(转出金额)
     *  借贷平衡：Cr = Σ转入 + 手续费 = 转出金额。
     */
    private void createVoucher(CashTransfer transfer) {
        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setSourceType("CASH_TRANSFER");
        request.setSourceId(transfer.getId());
        request.setSourceNo(transfer.getDocNo());
        request.setAmount(nvl(transfer.getFromAmount()));
        request.setSummary("提存转账 - " + transfer.getDocNo());
        request.setVoucherDate(transfer.getDocDate() != null ? transfer.getDocDate() : LocalDate.now());

        List<BusinessAccountingRequest.AccountingRequestItem> items = new ArrayList<>();
        BigDecimal toAmount = ZERO;

        List<CashTransferItem> itemList = cashTransferItemMapper.selectList(new LambdaQueryWrapper<CashTransferItem>()
                .eq(CashTransferItem::getTransferId, transfer.getId())
                .eq(CashTransferItem::getDeleted, 0));
        for (CashTransferItem item : itemList) {
            BigDecimal amt = nvl(item.getAmount());
            if (amt.compareTo(ZERO) <= 0) {
                continue;
            }
            BusinessAccountingRequest.AccountingRequestItem debit = new BusinessAccountingRequest.AccountingRequestItem();
            debit.setSummary(item.getToAccountName() != null ? item.getToAccountName() : "转入账户");
            debit.setSubjectCode(item.getToSubjectCode() != null ? item.getToSubjectCode() : subjectCodeOf(item.getToAccountType()));
            debit.setDebitAmount(amt);
            debit.setCreditAmount(ZERO);
            items.add(debit);
            toAmount = toAmount.add(amt);
        }
        // 手续费：计入管理费用，从转出账户支付
        BigDecimal fee = nvl(transfer.getFee());
        if (fee.compareTo(ZERO) > 0) {
            BusinessAccountingRequest.AccountingRequestItem feeDebit = new BusinessAccountingRequest.AccountingRequestItem();
            feeDebit.setSummary("提存手续费");
            feeDebit.setSubjectCode(SUBJECT_FEE);
            feeDebit.setDebitAmount(fee);
            feeDebit.setCreditAmount(ZERO);
            items.add(feeDebit);
        }
        // 贷方：转出账户 = Σ转入 + 手续费 = 转出金额
        BigDecimal creditTotal = toAmount.add(fee);
        BusinessAccountingRequest.AccountingRequestItem credit = new BusinessAccountingRequest.AccountingRequestItem();
        credit.setSummary(transfer.getFromAccountName() != null ? transfer.getFromAccountName() : "转出账户");
        credit.setSubjectCode(transfer.getFromSubjectCode() != null ? transfer.getFromSubjectCode() : subjectCodeOf(transfer.getFromAccountType()));
        credit.setDebitAmount(ZERO);
        credit.setCreditAmount(creditTotal);
        items.add(credit);

        request.setItems(items);
        businessAccountingService.createVoucherFromBusiness(request);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    /** 校验转出账户余额充足（P0/P1：账户余额不得扣成负数） */
    private void validateSufficientBalance(CashTransfer transfer) {
        if (transfer.getFromAccountId() == null) {
            return;
        }
        FinanceAccount account = financeAccountMapper.selectById(transfer.getFromAccountId());
        if (account == null) {
            throw BusinessException.badRequest("转出账户不存在: " + transfer.getFromAccountId());
        }
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : ZERO;
        if (balance.compareTo(nvl(transfer.getFromAmount())) < 0) {
            throw BusinessException.badRequest("转出账户余额不足：当前余额(" + balance + ") < 转出金额(" + nvl(transfer.getFromAmount()) + ")");
        }
    }
}
