package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.model.entity.FinanceTaxDeclaration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 税务申报Service接口
 */
public interface FinanceTaxDeclarationService {
    
    /**
     * 创建税务申报
     */
    boolean createDeclaration(FinanceTaxDeclaration declaration);
    
    /**
     * 查询申报列表
     */
    List<FinanceTaxDeclaration> listDeclarations(Integer taxType, String declarationPeriod, Integer status);
    
    /**
     * 查询申报详情
     */
    FinanceTaxDeclaration getDeclarationDetail(String declarationNo);
    
    /**
     * 更新申报状态
     */
    boolean updateDeclarationStatus(String declarationNo, Integer status);
    
    /**
     * 查询应缴税款统计
     */
    Object getTaxPayableStatistics(String period);
}
