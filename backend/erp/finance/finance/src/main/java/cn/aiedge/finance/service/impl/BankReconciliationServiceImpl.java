package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.BankReconciliation;
import cn.aiedge.finance.entity.BankStatement;
import cn.aiedge.finance.entity.BankUnmatchedItem;
import cn.aiedge.finance.mapper.BankReconciliationMapper;
import cn.aiedge.finance.mapper.BankStatementMapper;
import cn.aiedge.finance.mapper.BankUnmatchedItemMapper;
import cn.aiedge.finance.service.BankReconciliationService;
import cn.aiedge.finance.service.BankStatementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankReconciliationServiceImpl extends ServiceImpl<BankReconciliationMapper, BankReconciliation> implements BankReconciliationService {
    
    @Autowired
    private BankStatementMapper bankStatementMapper;
    
    @Autowired
    private BankStatementService bankStatementService;
    
    @Autowired
    private BankUnmatchedItemMapper bankUnmatchedItemMapper;
    
    @Override
    public List<BankReconciliation> listByAccountId(Long tenantId, Long accountId) {
        return baseMapper.listByAccountId(tenantId, accountId);
    }
    
    @Override
    public BankReconciliation getByAccountIdAndPeriod(Long tenantId, Long accountId, String period) {
        return baseMapper.getByAccountIdAndPeriod(tenantId, accountId, period);
    }
    
    @Override
    public Page<BankReconciliation> pageList(Long tenantId, Long accountId, Integer status, Page<BankReconciliation> page) {
        LambdaQueryWrapper<BankReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankReconciliation::getTenantId, tenantId)
               .eq(BankReconciliation::getDeleted, 0);
        if (accountId != null) {
            wrapper.eq(BankReconciliation::getAccountId, accountId);
        }
        if (status != null) {
            wrapper.eq(BankReconciliation::getStatus, status);
        }
        wrapper.orderByDesc(BankReconciliation::getReconciliationDate);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankReconciliation createReconciliation(Long tenantId, Long accountId, String period) {
        BankReconciliation existing = this.getByAccountIdAndPeriod(tenantId, accountId, period);
        if (existing != null) {
            throw new RuntimeException("该期间已存在对账记录");
        }
        BankReconciliation reconciliation = new BankReconciliation();
        reconciliation.setTenantId(tenantId);
        reconciliation.setAccountId(accountId);
        reconciliation.setPeriod(period);
        reconciliation.setReconciliationDate(LocalDate.now());
        reconciliation.setStatus(0);
        reconciliation.setBankBalance(BigDecimal.ZERO);
        reconciliation.setBookBalance(BigDecimal.ZERO);
        reconciliation.setDifference(BigDecimal.ZERO);
        reconciliation.setDepositInTransit(BigDecimal.ZERO);
        reconciliation.setOutstandingChecks(BigDecimal.ZERO);
        reconciliation.setBankErrors(BigDecimal.ZERO);
        reconciliation.setBookErrors(BigDecimal.ZERO);
        reconciliation.setAdjustedBankBalance(BigDecimal.ZERO);
        reconciliation.setAdjustedBookBalance(BigDecimal.ZERO);
        this.save(reconciliation);
        return reconciliation;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean calculateReconciliation(Long tenantId, Long reconciliationId) {
        BankReconciliation reconciliation = this.getById(reconciliationId);
        if (reconciliation == null) {
            throw new RuntimeException("对账记录不存在");
        }
        List<BankStatement> unmatchedStatements = bankStatementMapper.listByAccountIdAndStatus(
            tenantId, reconciliation.getAccountId(), 0);
        BigDecimal depositInTransit = BigDecimal.ZERO;
        BigDecimal outstandingChecks = BigDecimal.ZERO;
        for (BankStatement statement : unmatchedStatements) {
            if (statement.getTransactionType() == 1 || statement.getTransactionType() == 3) {
                depositInTransit = depositInTransit.add(statement.getAmount());
            } else {
                outstandingChecks = outstandingChecks.add(statement.getAmount());
            }
        }
        reconciliation.setDepositInTransit(depositInTransit);
        reconciliation.setOutstandingChecks(outstandingChecks);
        BigDecimal adjustedBankBalance = reconciliation.getBankBalance().add(depositInTransit).subtract(outstandingChecks);
        reconciliation.setAdjustedBankBalance(adjustedBankBalance);
        reconciliation.setAdjustedBookBalance(reconciliation.getBookBalance());
        reconciliation.setDifference(adjustedBankBalance.subtract(reconciliation.getBookBalance()));
        return this.updateById(reconciliation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long tenantId, Long reconciliationId) {
        BankReconciliation reconciliation = this.getById(reconciliationId);
        if (reconciliation == null) {
            throw new RuntimeException("对账记录不存在");
        }
        if (reconciliation.getDifference().compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("对账差异不为零，不能审核通过");
        }
        reconciliation.setStatus(1);
        return this.updateById(reconciliation);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReconciliation(Long tenantId, Long reconciliationId) {
        BankReconciliation reconciliation = this.getById(reconciliationId);
        if (reconciliation == null) {
            throw new RuntimeException("对账记录不存在");
        }
        if (reconciliation.getStatus() == 1) {
            throw new RuntimeException("已审核的对账记录不能删除");
        }
        LambdaQueryWrapper<BankUnmatchedItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankUnmatchedItem::getReconciliationId, reconciliationId);
        bankUnmatchedItemMapper.delete(wrapper);
        return this.removeById(reconciliationId);
    }
    
    @Override
    public Map<String, Object> getReconciliationDetail(Long tenantId, Long reconciliationId) {
        BankReconciliation reconciliation = this.getById(reconciliationId);
        if (reconciliation == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("reconciliation", reconciliation);
        result.put("unmatchedItems", this.getUnmatchedItems(tenantId, reconciliationId));
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getUnmatchedItems(Long tenantId, Long reconciliationId) {
        List<BankUnmatchedItem> items = bankUnmatchedItemMapper.listByReconciliationId(tenantId, reconciliationId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (BankUnmatchedItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("itemType", item.getItemType());
            itemMap.put("itemDate", item.getItemDate());
            itemMap.put("amount", item.getAmount());
            itemMap.put("summary", item.getSummary());
            itemMap.put("counterpartyName", item.getCounterpartyName());
            itemMap.put("counterpartyAccount", item.getCounterpartyAccount());
            result.add(itemMap);
        }
        return result;
    }
    
    @Override
    public BigDecimal calculateAdjustedBalance(Long tenantId, Long accountId, String period) {
        BankReconciliation reconciliation = this.getByAccountIdAndPeriod(tenantId, accountId, period);
        if (reconciliation == null) {
            return BigDecimal.ZERO;
        }
        return reconciliation.getAdjustedBankBalance();
    }
}