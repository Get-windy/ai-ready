package cn.aiedge.erp.finance.repository;

import cn.aiedge.erp.finance.model.entity.FinanceTaxDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 税务申报Repository接口
 */
@Repository
public interface FinanceTaxDeclarationRepository extends JpaRepository<FinanceTaxDeclaration, Long>, JpaSpecificationExecutor<FinanceTaxDeclaration> {
    
    /**
     * 根据申报编号查询
     */
    FinanceTaxDeclaration findByDeclarationNo(String declarationNo);
    
    /**
     * 根据税种查询
     */
    List<FinanceTaxDeclaration> findByTaxType(Integer taxType);
    
    /**
     * 根据申报期间查询
     */
    List<FinanceTaxDeclaration> findByDeclarationPeriod(String declarationPeriod);
    
    /**
     * 根据状态查询
     */
    List<FinanceTaxDeclaration> findByStatus(Integer status);
    
    /**
     * 根据纳税人识别号查询
     */
    List<FinanceTaxDeclaration> findByTaxpayerId(String taxpayerId);
    
    /**
     * 根据租户ID查询
     */
    List<FinanceTaxDeclaration> findByTenantId(String tenantId);
    
    /**
     * 查询最近的申报记录
     */
    List<FinanceTaxDeclaration> findByDeletedFlagOrderByDeclarationDateDesc(Integer deletedFlag);
}
