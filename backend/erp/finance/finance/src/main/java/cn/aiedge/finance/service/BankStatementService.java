package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.BankStatement;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BankStatementService extends IService<BankStatement> {
    
    List<BankStatement> listByAccountId(Long tenantId, Long accountId);
    
    List<BankStatement> listUnmatched(Long tenantId, Long accountId);
    
    Page<BankStatement> pageList(Long tenantId, Long accountId, Integer status, String startDate, String endDate, Page<BankStatement> page);
    
    boolean importStatements(Long tenantId, Long accountId, List<BankStatement> statements);
    
    boolean manualMatch(Long tenantId, Long statementId, Long transactionId);
    
    boolean autoMatch(Long tenantId, Long accountId);
    
    boolean unmatch(Long tenantId, Long statementId);
    
    boolean deleteStatement(Long tenantId, Long statementId);
    
    Map<String, BigDecimal> getAccountSummary(Long tenantId, Long accountId);
    
    Integer countUnmatched(Long tenantId, Long accountId);
    
    List<BankStatement> parseExcel(Long tenantId, Long accountId, byte[] fileData, String bankType);
}