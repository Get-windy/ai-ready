package cn.aiedge.erp.invoice.repository;

import cn.aiedge.erp.invoice.model.entity.PaymentRecord;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    
    List<PaymentRecord> findByInvoiceId(Long invoiceId);
    
    List<PaymentRecord> findByPaymentStatus(PaymentStatus paymentStatus);
    
    @Query("SELECT p FROM PaymentRecord p WHERE p.customerId = :customerId AND p.supplierId = :supplierId AND p.currency = :currency AND p.paymentStatus = 'PENDING'")
    List<PaymentRecord> findAvailableForAutoMatching(@Param("customerId") Long customerId, @Param("supplierId") Long supplierId, @Param("currency") String currency);
    
    @Modifying
    @Query("DELETE FROM PaymentRecord p WHERE p.paymentDate < :date AND p.paymentStatus = 'PENDING'")
    int deleteOldRecords(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(p) FROM PaymentRecord p WHERE p.matchingType = 'AUTO' AND p.paymentDate BETWEEN :startDate AND :endDate")
    int countAutoMatches(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(p) FROM PaymentRecord p WHERE p.matchingType = 'MANUAL' AND p.paymentDate BETWEEN :startDate AND :endDate")
    int countManualMatches(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}