package cn.aiedge.erp.invoice.mapper;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {

    @Select("SELECT * FROM invoice WHERE invoice_number = #{invoiceNumber}")
    Invoice findByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    @Select("SELECT * FROM invoice WHERE application_id = #{applicationId}")
    List<Invoice> findByApplicationId(@Param("applicationId") Long applicationId);

    @Select("SELECT * FROM invoice WHERE customer_id = #{customerId} ORDER BY invoice_date DESC")
    List<Invoice> findByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM invoice WHERE supplier_id = #{supplierId} ORDER BY invoice_date DESC")
    List<Invoice> findBySupplierId(@Param("supplierId") Long supplierId);

    @Select("SELECT * FROM invoice WHERE invoice_status = #{status} ORDER BY invoice_date DESC")
    List<Invoice> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM invoice WHERE payment_status = #{paymentStatus} ORDER BY due_date ASC")
    List<Invoice> findByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    @Select("SELECT * FROM invoice WHERE due_date < #{date} AND payment_status != 'PAID' ORDER BY due_date ASC")
    List<Invoice> findOverdueInvoices(@Param("date") LocalDate date);

    @Select("SELECT * FROM invoice WHERE invoice_date BETWEEN #{startDate} AND #{endDate} ORDER BY invoice_date DESC")
    List<Invoice> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT SUM(total_amount) FROM invoice WHERE customer_id = #{customerId} AND invoice_status != 'CANCELLED'")
    BigDecimal calculateTotalByCustomer(@Param("customerId") Long customerId);

    @Select("SELECT SUM(unpaid_amount) FROM invoice WHERE customer_id = #{customerId} AND payment_status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal calculateUnpaidByCustomer(@Param("customerId") Long customerId);

    @Select("SELECT SUM(total_amount) FROM invoice WHERE supplier_id = #{supplierId} AND invoice_status != 'CANCELLED'")
    BigDecimal calculateTotalBySupplier(@Param("supplierId") Long supplierId);

    @Select("SELECT SUM(unpaid_amount) FROM invoice WHERE supplier_id = #{supplierId} AND payment_status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal calculateUnpaidBySupplier(@Param("supplierId") Long supplierId);

    @Select("SELECT COUNT(*) FROM invoice WHERE due_date < #{date} AND payment_status != 'PAID'")
    Integer countOverdueInvoices(@Param("date") LocalDate date);

    @Select("SELECT * FROM invoice WHERE original_invoice_id = #{originalInvoiceId}")
    List<Invoice> findCreditNotesByOriginal(@Param("originalInvoiceId") Long originalInvoiceId);
}