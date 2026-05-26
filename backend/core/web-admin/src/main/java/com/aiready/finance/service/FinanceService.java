package com.aiready.finance.service;

import com.aiready.finance.dto.*;
import com.aiready.finance.entity.AccountSubject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 财务管理服务接口
 */
public interface FinanceService extends IService<AccountSubject> {
    
    // ==================== 会计科目管理 ====================
    
    /**
     * 创建会计科目
     */
    AccountSubjectDTO createSubject(AccountSubjectSaveRequest request, Long operatorId);
    
    /**
     * 更新会计科目
     */
    AccountSubjectDTO updateSubject(Long subjectId, AccountSubjectSaveRequest request, Long operatorId);
    
    /**
     * 删除会计科目
     */
    void deleteSubject(Long subjectId, Long operatorId);
    
    /**
     * 批量删除会计科目
     */
    void batchDeleteSubjects(List<Long> subjectIds, Long operatorId);
    
    /**
     * 获取会计科目详情
     */
    AccountSubjectDTO getSubjectById(Long subjectId);
    
    /**
     * 根据编码获取会计科目
     */
    AccountSubjectDTO getSubjectByCode(String subjectCode);
    
    /**
     * 查询会计科目列表
     */
    IPage<AccountSubjectDTO> querySubjects(AccountSubjectQueryRequest request);
    
    /**
     * 获取科目树形结构
     */
    List<AccountSubjectDTO> getSubjectTree();
    
    /**
     * 获取所有明细科目
     */
    List<AccountSubjectDTO> getDetailSubjects();
    
    /**
     * 验证科目编码唯一性
     */
    boolean validateSubjectCode(String subjectCode, Long excludeId);
    
    // ==================== 凭证管理 ====================
    
    /**
     * 创建凭证
     */
    VoucherDTO createVoucher(VoucherSaveRequest request, Long operatorId);
    
    /**
     * 更新凭证
     */
    VoucherDTO updateVoucher(Long voucherId, VoucherSaveRequest request, Long operatorId);
    
    /**
     * 删除凭证
     */
    void deleteVoucher(Long voucherId, Long operatorId);
    
    /**
     * 获取凭证详情
     */
    VoucherDTO getVoucherById(Long voucherId);
    
    /**
     * 查询凭证列表
     */
    IPage<VoucherDTO> queryVouchers(VoucherQueryRequest request);
    
    /**
     * 审核凭证
     */
    void reviewVoucher(Long voucherId, Long operatorId);
    
    /**
     * 取消审核
     */
    void cancelReview(Long voucherId, Long operatorId);
    
    /**
     * 记账
     */
    void bookkeeping(Long voucherId, Long operatorId);
    
    /**
     * 取消记账
     */
    void cancelBookkeeping(Long voucherId, Long operatorId);
    
    /**
     * 作废凭证
     */
    void cancelVoucher(Long voucherId, Long operatorId);
    
    // ==================== 账簿管理 ====================
    
    /**
     * 查询明细账
     */
    IPage<LedgerDTO> queryDetailLedger(LedgerQueryRequest request);
    
    /**
     * 查询总账
     */
    IPage<LedgerDTO> queryGeneralLedger(LedgerQueryRequest request);
    
    /**
     * 获取科目余额
     */
    BigDecimal getSubjectBalance(Long subjectId, String accountingPeriod);
    
    // ==================== 财务报表 ====================
    
    /**
     * 生成资产负债表
     */
    Map<String, Object> generateBalanceSheet(String accountingPeriod);
    
    /**
     * 生成利润表
     */
    Map<String, Object> generateIncomeStatement(String startPeriod, String endPeriod);
    
    /**
     * 生成现金流量表
     */
    Map<String, Object> generateCashFlowStatement(String startPeriod, String endPeriod);
}
