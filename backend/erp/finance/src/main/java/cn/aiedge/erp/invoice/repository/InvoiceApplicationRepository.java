package cn.aiedge.erp.invoice.repository;

import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票申请存储库接口
 */
@Repository
public interface InvoiceApplicationRepository extends JpaRepository<InvoiceApplication, Long> {
    
    /**
     * 根据状态查找发票申请
     * 
     * @param status 申请状态
     * @return 发票申请列表
     */
    List<InvoiceApplication> findByStatus(InvoiceStatus status);
    
    /**
     * 根据客户ID查找发票申请
     * 
     * @param customerId 客户ID
     * @return 发票申请列表
     */
    List<InvoiceApplication> findByCustomerId(Long customerId);
    
    /**
     * 根据供应商ID查找发票申请
     * 
     * @param supplierId 供应商ID
     * @return 发票申请列表
     */
    List<InvoiceApplication> findBySupplierId(Long supplierId);
    
    /**
     * 根据日期范围查找发票申请
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 发票申请列表
     */
    List<InvoiceApplication> findByApplicationDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 分页查找发票申请
     * 
     * @param pageable 分页参数
     * @return 发票申请分页
     */
    Page<InvoiceApplication> findAll(Pageable pageable);
    
    /**
     * 搜索发票申请
     * 
     * @param keyword 关键词
     * @return 发票申请列表
     */
    @Query("SELECT a FROM InvoiceApplication a WHERE " +
           "a.applicationNumber LIKE %:keyword% OR " +
           "a.customerName LIKE %:keyword% OR " +
           "a.supplierName LIKE %:keyword% OR " +
           "a.notes LIKE %:keyword%")
    List<InvoiceApplication> searchInvoiceApplications(@Param("keyword") String keyword);
}