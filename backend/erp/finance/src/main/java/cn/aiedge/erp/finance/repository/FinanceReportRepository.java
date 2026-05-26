package cn.aiedge.erp.finance.repository;

import cn.aiedge.erp.finance.model.entity.FinanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 财务报表Repository接口
 */
@Repository
public interface FinanceReportRepository extends JpaRepository<FinanceReport, Long>, JpaSpecificationExecutor<FinanceReport> {
    
    /**
     * 根据报表编号查询
     */
    FinanceReport findByReportNo(String reportNo);
    
    /**
     * 根据报表类型查询
     */
    List<FinanceReport> findByReportType(Integer reportType);
    
    /**
     * 根据报表期间查询
     */
    List<FinanceReport> findByReportPeriod(String reportPeriod);
    
    /**
     * 根据状态查询
     */
    List<FinanceReport> findByStatus(Integer status);
    
    /**
     * 根据租户ID查询
     */
    List<FinanceReport> findByTenantId(String tenantId);
    
    /**
     * 查询最新的报表
     */
    List<FinanceReport> findByDeletedFlagOrderByGeneratedAtDesc(Integer deletedFlag);
}
