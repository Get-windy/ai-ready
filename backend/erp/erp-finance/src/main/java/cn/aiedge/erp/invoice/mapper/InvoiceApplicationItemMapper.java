package cn.aiedge.erp.invoice.mapper;

import cn.aiedge.erp.invoice.model.entity.InvoiceApplicationItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface InvoiceApplicationItemMapper extends BaseMapper<InvoiceApplicationItem> {

    @Select("SELECT * FROM invoice_application_item WHERE invoice_application_id = #{applicationId} ORDER BY id")
    List<InvoiceApplicationItem> findByApplicationId(@Param("applicationId") Long applicationId);

    @Select("SELECT * FROM invoice_application_item WHERE product_id = #{productId}")
    List<InvoiceApplicationItem> findByProductId(@Param("productId") Long productId);

    @Select("SELECT SUM(quantity) FROM invoice_application_item WHERE invoice_application_id = #{applicationId}")
    BigDecimal sumQuantityByApplication(@Param("applicationId") Long applicationId);

    @Select("SELECT SUM(amount) FROM invoice_application_item WHERE invoice_application_id = #{applicationId}")
    BigDecimal sumAmountByApplication(@Param("applicationId") Long applicationId);

    @Select("SELECT COUNT(*) FROM invoice_application_item WHERE invoice_application_id = #{applicationId}")
    Integer countByApplication(@Param("applicationId") Long applicationId);
}