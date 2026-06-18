package cn.aiedge.erp.invoice.mapper;

import cn.aiedge.erp.invoice.model.entity.InvoiceItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InvoiceItemMapper extends BaseMapper<InvoiceItem> {

    @Select("SELECT * FROM invoice_item WHERE invoice_id = #{invoiceId} ORDER BY id")
    List<InvoiceItem> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Select("SELECT * FROM invoice_item WHERE product_id = #{productId}")
    List<InvoiceItem> findByProductId(@Param("productId") Long productId);

    @Select("SELECT SUM(quantity) FROM invoice_item WHERE invoice_id = #{invoiceId}")
    java.math.BigDecimal sumQuantityByInvoice(@Param("invoiceId") Long invoiceId);

    @Select("SELECT SUM(amount) FROM invoice_item WHERE invoice_id = #{invoiceId}")
    java.math.BigDecimal sumAmountByInvoice(@Param("invoiceId") Long invoiceId);
}