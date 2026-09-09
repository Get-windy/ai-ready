package cn.aiedge.erp.invoice.repository;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.InvoiceType;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import cn.aiedge.erp.invoice.model.enums.MatchingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票存储库接口
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    /**
     * 根据发票号码查找发票
     * 
     * @param invoiceNumber 发票号码
     * @return 发票对象（如果存在）
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    /**
     * 根据状态查找发票
     * 
     * @param status 发票状态
     * @return 发票列表
     */
    List<Invoice> findByInvoiceStatus(InvoiceStatus status);
    
    /**
     * 根据付款状态查找发票
     * 
     * @param paymentStatus 付款状态
     * @return 发票列表
     */
    List<Invoice> findByPaymentStatus(PaymentStatus paymentStatus);
    
    /**
     * 根据客户ID查找发票
     * 
     * @param customerId 客户ID
     * @return 发票列表
     */
    List<Invoice> findByCustomerId(Long customerId);
    
    /**
     * 根据供应商ID查找发票
     * 
     * @param supplierId 供应商ID
     * @return 发票列表
     */
    List<Invoice> findBySupplierId(Long supplierId);
    
    /**
     * 根据日期范围查找发票
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 发票列表
     */
    List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据发票日期统计数量
     * 
     * @param invoiceDate 发票日期
     * @return 发票数量
     */
    long countByInvoiceDate(LocalDate invoiceDate);
    
    /**
     * 根据金额范围查找发票
     * 
     * @param minAmount 最小金额
     * @param maxAmount 最大金额
     * @return 发票列表
     */
    @Query("SELECT i FROM Invoice i WHERE i.totalAmount >= :minAmount AND i.totalAmount <= :maxAmount")
    List<Invoice> findByAmountRange(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);
    
    /**
     * 分页查找发票
     * 
     * @param pageable 分页参数
     * @return 发票分页
     */
    Page<Invoice> findAll(Pageable pageable);
    
    /**
     * 搜索发票
     * 
     * @param keyword 关键词
     * @return 发票列表
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "i.invoiceNumber LIKE %:keyword% OR " +
           "i.customerName LIKE %:keyword% OR " +
           "i.supplierName LIKE %:keyword% OR " +
           "i.notes LIKE %:keyword%")
    List<Invoice> searchInvoices(@Param("keyword") String keyword);
    
    /**
     * 分页查询发票（按方向/类型 + 单据/发票信息过滤）
     * 用于「按单付款-发票查询」销售/采购发票查询 Tab。
     *
     * @param types       发票类型集合（销售类 / 采购类，由调用方按方向注入）
     * @param invoiceNumber 发票号码（模糊，可空）
     * @param partnerName   往来单位名称（客户/供应商，模糊，可空）
     * @param issuedByName  开票人（模糊，可空）
     * @param startDate     发票日期起（可空）
     * @param endDate       发票日期止（可空）
     * @param pageable      分页参数
     * @return 发票分页
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "i.invoiceType IN :types " +
           "AND (:invoiceNumber = '' OR i.invoiceNumber LIKE concat('%', :invoiceNumber, '%')) " +
           "AND (:partnerName = '' OR i.customerName LIKE concat('%', :partnerName, '%') OR i.supplierName LIKE concat('%', :partnerName, '%')) " +
           "AND (:issuedByName = '' OR i.issuedByName LIKE concat('%', :issuedByName, '%')) " +
           "AND i.invoiceDate >= :startDate AND i.invoiceDate <= :endDate")
    Page<Invoice> findByTypesAndFilters(@Param("types") List<String> types,
                                        @Param("invoiceNumber") String invoiceNumber,
                                        @Param("partnerName") String partnerName,
                                        @Param("issuedByName") String issuedByName,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        Pageable pageable);

    /**
     * 批量更新逾期状态
     *
     * @return 更新的记录数
     */
    @Query(value = "UPDATE invoice SET overdue_days = DATEDIFF(CURDATE(), due_date), " +
                   "payment_status = 'OVERDUE' " +
                   "WHERE due_date < CURDATE() AND payment_status IN ('PENDING', 'PARTIALLY_PAID')",
           nativeQuery = true)
    int batchUpdateOverdueStatus();
    
    List<Invoice> findByMatchingStatus(MatchingStatus matchingStatus);
    
    @Query("SELECT i FROM Invoice i WHERE i.customerId = :customerId AND i.matchingStatus = :matchingStatus AND i.invoiceDate BETWEEN :startDate AND :endDate")
    List<Invoice> findUnmatchedByCustomerId(@Param("customerId") Long customerId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.supplierId = :supplierId AND i.matchingStatus = :matchingStatus AND i.invoiceDate BETWEEN :startDate AND :endDate")
    List<Invoice> findUnmatchedBySupplierId(@Param("supplierId") Long supplierId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.matchingStatus = :matchingStatus AND i.invoiceDate BETWEEN :startDate AND :endDate")
    List<Invoice> findUnmatchedInvoices(@Param("matchingStatus") MatchingStatus matchingStatus, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.matchingStatus = 'UNMATCHED' AND i.invoiceDate BETWEEN :startDate AND :endDate")
    List<Invoice> findUnmatchedInvoices(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.dueDate BETWEEN :startDate AND :endDate AND i.matchingStatus = 'UNMATCHED' AND i.invoiceStatus IN ('VALIDATED', 'APPROVED')")
    List<Invoice> findDueInvoicesForAutoMatching(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}