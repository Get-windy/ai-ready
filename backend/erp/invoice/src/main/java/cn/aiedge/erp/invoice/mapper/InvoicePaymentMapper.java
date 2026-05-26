package cn.aiedge.erp.invoice.mapper;

import cn.aiedge.erp.invoice.model.entity.InvoicePayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InvoicePaymentMapper extends BaseMapper<InvoicePayment> {

    @Select("SELECT * FROM invoice_payment WHERE invoice_id = #{invoiceId} ORDER BY payment_date DESC")
    List<InvoicePayment> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Select("SELECT * FROM invoice_payment WHERE payment_number = #{paymentNumber}")
    InvoicePayment findByPaymentNumber(@Param("paymentNumber") String paymentNumber);

    @Select("SELECT SUM(amount) FROM invoice_payment WHERE invoice_id = #{invoiceId}")
    BigDecimal sumPaidAmountByInvoice(@Param("invoiceId") Long invoiceId);

    @Select("SELECT * FROM invoice_payment WHERE payment_date BETWEEN #{startDate} AND #{endDate} ORDER BY payment_date DESC")
    List<InvoicePayment> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM invoice_payment WHERE invoice_id = #{invoiceId}")
    Integer countByInvoice(@Param("invoiceId") Long invoiceId);
}