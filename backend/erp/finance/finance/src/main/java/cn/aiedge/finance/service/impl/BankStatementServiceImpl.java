package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.BankStatement;
import cn.aiedge.finance.enums.BankStatementStatus;
import cn.aiedge.finance.mapper.BankStatementMapper;
import cn.aiedge.finance.service.BankStatementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankStatementServiceImpl extends ServiceImpl<BankStatementMapper, BankStatement> implements BankStatementService {
    
    @Override
    public List<BankStatement> listByAccountId(Long tenantId, Long accountId) {
        return baseMapper.listByAccountId(tenantId, accountId);
    }
    
    @Override
    public List<BankStatement> listUnmatched(Long tenantId, Long accountId) {
        return baseMapper.listByAccountIdAndStatus(tenantId, accountId, BankStatementStatus.UNMATCHED.getCode());
    }
    
    @Override
    public Page<BankStatement> pageList(Long tenantId, Long accountId, Integer status, String startDate, String endDate, Page<BankStatement> page) {
        LambdaQueryWrapper<BankStatement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankStatement::getTenantId, tenantId)
               .eq(BankStatement::getDeleted, 0)
               .eq(BankStatement::getAccountId, accountId);
        if (status != null) {
            wrapper.eq(BankStatement::getStatus, status);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(BankStatement::getTransactionDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(BankStatement::getTransactionDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(BankStatement::getTransactionDate);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importStatements(Long tenantId, Long accountId, List<BankStatement> statements) {
        Integer maxBatch = baseMapper.getMaxImportBatch(tenantId);
        int nextBatch = (maxBatch != null ? maxBatch : 0) + 1;
        int rowNo = 1;
        for (BankStatement statement : statements) {
            statement.setTenantId(tenantId);
            statement.setAccountId(accountId);
            statement.setStatus(BankStatementStatus.UNMATCHED.getCode());
            statement.setImportBatch(nextBatch);
            statement.setRowNo(rowNo++);
            this.save(statement);
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean manualMatch(Long tenantId, Long statementId, Long transactionId) {
        BankStatement statement = this.getById(statementId);
        if (statement == null) {
            throw new RuntimeException("银行流水不存在");
        }
        statement.setStatus(BankStatementStatus.MANUAL_MATCHED.getCode());
        statement.setMatchedTransactionId(transactionId);
        statement.setMatchedDate(LocalDate.now());
        return this.updateById(statement);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoMatch(Long tenantId, Long accountId) {
        List<BankStatement> unmatchedStatements = this.listUnmatched(tenantId, accountId);
        for (BankStatement statement : unmatchedStatements) {
            LambdaQueryWrapper<BankStatement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BankStatement::getTenantId, tenantId)
                   .eq(BankStatement::getAccountId, accountId)
                   .eq(BankStatement::getAmount, statement.getAmount())
                   .eq(BankStatement::getTransactionDate, statement.getTransactionDate())
                   .eq(BankStatement::getStatus, BankStatementStatus.UNMATCHED.getCode())
                   .ne(BankStatement::getId, statement.getId());
            List<BankStatement> matches = this.list(wrapper);
            if (!matches.isEmpty()) {
                statement.setStatus(BankStatementStatus.AUTO_MATCHED.getCode());
                statement.setMatchedDate(LocalDate.now());
                this.updateById(statement);
            }
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unmatch(Long tenantId, Long statementId) {
        BankStatement statement = this.getById(statementId);
        if (statement == null) {
            throw new RuntimeException("银行流水不存在");
        }
        statement.setStatus(BankStatementStatus.UNMATCHED.getCode());
        statement.setMatchedTransactionId(null);
        statement.setMatchedDate(null);
        return this.updateById(statement);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStatement(Long tenantId, Long statementId) {
        BankStatement statement = this.getById(statementId);
        if (statement == null) {
            throw new RuntimeException("银行流水不存在");
        }
        if (statement.getStatus() != BankStatementStatus.UNMATCHED.getCode()) {
            throw new RuntimeException("已对账的流水不能删除");
        }
        return this.removeById(statementId);
    }
    
    @Override
    public Map<String, BigDecimal> getAccountSummary(Long tenantId, Long accountId) {
        BigDecimal deposit = baseMapper.sumDepositByAccountId(tenantId, accountId);
        BigDecimal withdraw = baseMapper.sumWithdrawByAccountId(tenantId, accountId);
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("totalDeposit", deposit != null ? deposit : BigDecimal.ZERO);
        result.put("totalWithdraw", withdraw != null ? withdraw : BigDecimal.ZERO);
        result.put("netAmount", (deposit != null ? deposit : BigDecimal.ZERO).subtract(withdraw != null ? withdraw : BigDecimal.ZERO));
        return result;
    }
    
    @Override
    public Integer countUnmatched(Long tenantId, Long accountId) {
        return baseMapper.countUnmatchedByAccountId(tenantId, accountId);
    }
    
    @Override
    public List<BankStatement> parseExcel(Long tenantId, Long accountId, byte[] fileData, String bankType) {
        return new ArrayList<>();
    }
}