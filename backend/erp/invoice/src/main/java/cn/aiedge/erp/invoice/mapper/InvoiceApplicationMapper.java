package cn.aiedge.erp.invoice.mapper;

import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InvoiceApplicationMapper extends BaseMapper<InvoiceApplication> {

    @Select("SELECT * FROM invoice_application WHERE application_number = #{applicationNumber}")
    InvoiceApplication findByApplicationNumber(@Param("applicationNumber") String applicationNumber);

    @Select("SELECT * FROM invoice_application WHERE order_id = #{orderId}")
    List<InvoiceApplication> findByOrderId(@Param("orderId") Long orderId);

    @Select("SELECT * FROM invoice_application WHERE customer_id = #{customerId} ORDER BY application_date DESC")
    List<InvoiceApplication> findByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM invoice_application WHERE supplier_id = #{supplierId} ORDER BY application_date DESC")
    List<InvoiceApplication> findBySupplierId(@Param("supplierId") Long supplierId);

    @Select("SELECT * FROM invoice_application WHERE status = #{status} ORDER BY application_date DESC")
    List<InvoiceApplication> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM invoice_application WHERE applicant_id = #{applicantId} ORDER BY application_date DESC")
    List<InvoiceApplication> findByApplicantId(@Param("applicantId") Long applicantId);

    @Select("SELECT * FROM invoice_application WHERE application_date BETWEEN #{startDate} AND #{endDate} ORDER BY application_date DESC")
    List<InvoiceApplication> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM invoice_application WHERE invoice_generated = false AND status = 'APPROVED'")
    List<InvoiceApplication> findPendingGeneration();

    @Select("SELECT COUNT(*) FROM invoice_application WHERE status = #{status}")
    Integer countByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM invoice_application WHERE applicant_id = #{applicantId} AND status IN ('DRAFT', 'SUBMITTED', 'IN_APPROVAL')")
    Integer countPendingByApplicant(@Param("applicantId") Long applicantId);
}