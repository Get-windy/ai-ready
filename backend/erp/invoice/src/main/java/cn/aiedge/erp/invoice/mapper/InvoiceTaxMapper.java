package cn.aiedge.erp.invoice.mapper;

import cn.aiedge.erp.invoice.model.entity.InvoiceTax;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface InvoiceTaxMapper extends BaseMapper<InvoiceTax> {

    @Select("SELECT * FROM invoice_tax WHERE invoice_id = #{invoiceId} ORDER BY id")
    List<InvoiceTax> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Select("SELECT SUM(tax_amount) FROM invoice_tax WHERE invoice_id = #{invoiceId}")
    BigDecimal sumTaxAmountByInvoice(@Param("invoiceId") Long invoiceId);

    @Select("SELECT * FROM invoice_tax WHERE invoice_id = #{invoiceId} AND tax_type = #{taxType}")
    InvoiceTax findByInvoiceAndTaxType(@Param("invoiceId") Long invoiceId, @Param("taxType") String taxType);
}