package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AccountBalance;
import cn.aiedge.finance.entity.AccountPeriod;
import cn.aiedge.finance.entity.AccountSubject;
import cn.aiedge.finance.entity.Voucher;
import cn.aiedge.finance.entity.VoucherEntry;
import cn.aiedge.finance.enums.VoucherStatus;
import cn.aiedge.finance.mapper.AccountBalanceMapper;
import cn.aiedge.finance.mapper.AccountPeriodMapper;
import cn.aiedge.finance.mapper.AccountSubjectMapper;
import cn.aiedge.finance.mapper.VoucherEntryMapper;
import cn.aiedge.finance.mapper.VoucherMapper;
import cn.aiedge.finance.service.AccountBalanceService;
import cn.aiedge.finance.service.VoucherEntryService;
import cn.aiedge.finance.service.VoucherService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {
    
    @Autowired
    private VoucherEntryMapper voucherEntryMapper;
    
    @Autowired
    private VoucherEntryService voucherEntryService;
    
    @Autowired
    private AccountSubjectMapper accountSubjectMapper;
    
    @Autowired
    private AccountBalanceMapper accountBalanceMapper;
    
    @Autowired
    private AccountBalanceService accountBalanceService;
    
    @Autowired
    private AccountPeriodMapper accountPeriodMapper;
    
    @Override
    public List<Voucher> listByPeriod(Long tenantId, String period) {
        return baseMapper.listByPeriod(tenantId, period);
    }
    
    @Override
    public Voucher getByVoucherNo(Long tenantId, String voucherNo) {
        return baseMapper.getByVoucherNo(tenantId, voucherNo);
    }
    
    @Override
    public Voucher getBySource(Long tenantId, String sourceType, Long sourceId) {
        return baseMapper.getBySource(tenantId, sourceType, sourceId);
    }
    
    @Override
    public Page<Voucher> pageList(Long tenantId, String period, String voucherNo, Integer voucherType, Integer status, String startDate, String endDate, Page<Voucher> page) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getTenantId, tenantId)
               .eq(Voucher::getDeleted, 0);
        if (period != null && !period.isEmpty()) {
            wrapper.eq(Voucher::getPeriod, period);
        }
        if (voucherNo != null && !voucherNo.isEmpty()) {
            wrapper.like(Voucher::getVoucherNo, voucherNo);
        }
        if (voucherType != null) {
            wrapper.eq(Voucher::getVoucherType, voucherType);
        }
        if (status != null) {
            wrapper.eq(Voucher::getStatus, status);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Voucher::getVoucherDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Voucher::getVoucherDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(Voucher::getVoucherDate).orderByDesc(Voucher::getVoucherNo);
        return this.page(page, wrapper);
    }
    
    @Override
    public Voucher getDetail(Long tenantId, Long voucherId) {
        Voucher voucher = this.getById(voucherId);
        if (voucher != null) {
            List<VoucherEntry> entries = voucherEntryMapper.listByVoucherId(voucherId);
            voucher.setEntries(entries);
        }
        return voucher;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createVoucher(Voucher voucher) {
        AccountPeriod currentPeriod = accountPeriodMapper.getCurrentPeriod(voucher.getTenantId());
        if (currentPeriod == null || currentPeriod.getStatus() != 1) {
            throw new RuntimeException("当前会计期间未开账");
        }
        String period = String.format("%04d%02d", currentPeriod.getYear(), currentPeriod.getMonth());
        voucher.setPeriod(period);
        voucher.setVoucherDate(LocalDate.now());
        voucher.setStatus(VoucherStatus.DRAFT.getCode());
        voucher.setPrinted(0);
        voucher.setPrintCount(0);
        if (voucher.getVoucherNo() == null || voucher.getVoucherNo().isEmpty()) {
            voucher.setVoucherNo(generateVoucherNo(voucher.getTenantId(), period, voucher.getWord()));
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        if (voucher.getEntries() != null) {
            for (VoucherEntry entry : voucher.getEntries()) {
                totalDebit = totalDebit.add(entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO);
                totalCredit = totalCredit.add(entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO);
            }
        }
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setEntryCount(voucher.getEntries() != null ? voucher.getEntries().size() : 0);
        this.save(voucher);
        if (voucher.getEntries() != null && !voucher.getEntries().isEmpty()) {
            voucherEntryService.saveEntries(voucher.getId(), voucher.getEntries());
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVoucher(Voucher voucher) {
        if (!checkVoucherCanEdit(voucher.getTenantId(), voucher.getId())) {
            throw new RuntimeException("凭证状态不允许编辑");
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        if (voucher.getEntries() != null) {
            for (VoucherEntry entry : voucher.getEntries()) {
                totalDebit = totalDebit.add(entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO);
                totalCredit = totalCredit.add(entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO);
            }
        }
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setEntryCount(voucher.getEntries() != null ? voucher.getEntries().size() : 0);
        this.updateById(voucher);
        voucherEntryService.deleteByVoucherId(voucher.getId());
        if (voucher.getEntries() != null && !voucher.getEntries().isEmpty()) {
            voucherEntryService.saveEntries(voucher.getId(), voucher.getEntries());
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteVoucher(Long tenantId, Long voucherId) {
        if (!checkVoucherCanEdit(tenantId, voucherId)) {
            throw new RuntimeException("凭证状态不允许删除");
        }
        voucherEntryService.deleteByVoucherId(voucherId);
        return this.removeById(voucherId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitForReview(Long tenantId, Long voucherId) {
        Voucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        if (voucher.getStatus() != VoucherStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的凭证才能提交审核");
        }
        if (!checkBalance(voucher)) {
            throw new RuntimeException("凭证借贷不平衡，不能提交审核");
        }
        voucher.setStatus(VoucherStatus.SUBMITTED.getCode());
        voucher.setPreparedTime(LocalDateTime.now());
        return this.updateById(voucher);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long tenantId, Long voucherId) {
        Voucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        if (voucher.getStatus() != VoucherStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("只有待审核状态的凭证才能审核");
        }
        voucher.setStatus(VoucherStatus.APPROVED.getCode());
        voucher.setReviewedTime(LocalDateTime.now());
        return this.updateById(voucher);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long tenantId, Long voucherId, String reason) {
        Voucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        if (voucher.getStatus() != VoucherStatus.SUBMITTED.getCode()) {
            throw new RuntimeException("只有待审核状态的凭证才能驳回");
        }
        voucher.setStatus(VoucherStatus.REJECTED.getCode());
        voucher.setReviewedTime(LocalDateTime.now());
        voucher.setRemark(reason);
        return this.updateById(voucher);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean post(Long tenantId, Long voucherId) {
        Voucher voucher = this.getDetail(tenantId, voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        if (voucher.getStatus() != VoucherStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审核状态的凭证才能记账");
        }
        if (!checkBalance(voucher)) {
            throw new RuntimeException("凭证借贷不平衡，不能记账");
        }
        for (VoucherEntry entry : voucher.getEntries()) {
            BigDecimal debit = entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;
            accountBalanceService.updateBalance(tenantId, entry.getSubjectId(), voucher.getPeriod(), debit, credit);
        }
        voucher.setStatus(VoucherStatus.POSTED.getCode());
        voucher.setPostedTime(LocalDateTime.now());
        return this.updateById(voucher);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidVoucher(Long tenantId, Long voucherId, String reason) {
        Voucher voucher = this.getDetail(tenantId, voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        if (voucher.getStatus() == VoucherStatus.VOIDED.getCode()) {
            throw new RuntimeException("凭证已作废");
        }
        if (voucher.getStatus() == VoucherStatus.POSTED.getCode()) {
            for (VoucherEntry entry : voucher.getEntries()) {
                BigDecimal debit = entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
                BigDecimal credit = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;
                accountBalanceService.updateBalance(tenantId, entry.getSubjectId(), voucher.getPeriod(), debit.negate(), credit.negate());
            }
        }
        voucher.setStatus(VoucherStatus.VOIDED.getCode());
        voucher.setVoidReason(reason);
        voucher.setVoidTime(LocalDateTime.now());
        return this.updateById(voucher);
    }
    
    @Override
    public boolean print(Long tenantId, Long voucherId) {
        Voucher voucher = this.getById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        voucher.setPrinted(1);
        voucher.setPrintCount(voucher.getPrintCount() + 1);
        return this.updateById(voucher);
    }
    
    @Override
    public String generateVoucherNo(Long tenantId, String period, String word) {
        String prefix = period + "-";
        if (word != null && !word.isEmpty()) {
            prefix = word + "-";
        }
        Integer maxNo = baseMapper.getMaxWordNo(tenantId, period, word != null ? word : "");
        int nextNo = (maxNo != null ? maxNo : 0) + 1;
        return prefix + String.format("%04d", nextNo);
    }
    
    @Override
    public boolean checkBalance(Voucher voucher) {
        BigDecimal totalDebit = voucher.getTotalDebit() != null ? voucher.getTotalDebit() : BigDecimal.ZERO;
        BigDecimal totalCredit = voucher.getTotalCredit() != null ? voucher.getTotalCredit() : BigDecimal.ZERO;
        return totalDebit.compareTo(totalCredit) == 0;
    }
    
    @Override
    public boolean checkVoucherCanEdit(Long tenantId, Long voucherId) {
        Voucher voucher = this.getById(voucherId);
        if (voucher == null) return false;
        return voucher.getStatus() == VoucherStatus.DRAFT.getCode() 
            || voucher.getStatus() == VoucherStatus.REJECTED.getCode();
    }
    
    @Override
    public boolean checkVoucherCanPost(Long tenantId, Long voucherId) {
        Voucher voucher = this.getById(voucherId);
        if (voucher == null) return false;
        return voucher.getStatus() == VoucherStatus.APPROVED.getCode() && checkBalance(voucher);
    }
    
    @Override
    public Map<String, BigDecimal> getPeriodSummary(Long tenantId, String period) {
        List<Voucher> vouchers = this.listByPeriod(tenantId, period);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int postedCount = 0;
        int unpostedCount = 0;
        for (Voucher voucher : vouchers) {
            if (voucher.getStatus() == VoucherStatus.POSTED.getCode()) {
                totalDebit = totalDebit.add(voucher.getTotalDebit());
                totalCredit = totalCredit.add(voucher.getTotalCredit());
                postedCount++;
            } else {
                unpostedCount++;
            }
        }
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("totalDebit", totalDebit);
        result.put("totalCredit", totalCredit);
        result.put("postedCount", BigDecimal.valueOf(postedCount));
        result.put("unpostedCount", BigDecimal.valueOf(unpostedCount));
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Voucher> generateFromInvoice(Long tenantId, Long invoiceId) {
        return new ArrayList<>();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Voucher> generateFromPayment(Long tenantId, Long paymentId) {
        return new ArrayList<>();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Voucher> generateFromReceipt(Long tenantId, Long receiptId) {
        return new ArrayList<>();
    }
}