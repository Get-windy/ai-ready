package cn.aiedge.erp.supplier.mapper;

import cn.aiedge.erp.supplier.model.entity.InquiryQuotationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InquiryQuotationMapper extends BaseMapper<InquiryQuotationEntity> {
    
    @Select("SELECT * FROM erp_inquiry_quotation WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted = 0 ORDER BY create_time DESC")
    List<InquiryQuotationEntity> findBySupplierId(@Param("tenantId") String tenantId, @Param("supplierId") Long supplierId);
    
    @Select("SELECT * FROM erp_inquiry_quotation WHERE tenant_id = #{tenantId} AND inquiry_no = #{inquiryNo} AND deleted = 0")
    InquiryQuotationEntity findByInquiryNo(@Param("tenantId") String tenantId, @Param("inquiryNo") String inquiryNo);
    
    @Select("SELECT * FROM erp_inquiry_quotation WHERE tenant_id = #{tenantId} AND quotation_no = #{quotationNo} AND deleted = 0")
    InquiryQuotationEntity findByQuotationNo(@Param("tenantId") String tenantId, @Param("quotationNo") String quotationNo);
    
    @Select("SELECT * FROM erp_inquiry_quotation WHERE tenant_id = #{tenantId} AND inquiry_status = #{status} AND deleted = 0")
    List<InquiryQuotationEntity> findByInquiryStatus(@Param("tenantId") String tenantId, @Param("status") Integer status);
    
    @Select("SELECT * FROM erp_inquiry_quotation WHERE tenant_id = #{tenantId} AND quotation_status = #{status} AND deleted = 0")
    List<InquiryQuotationEntity> findByQuotationStatus(@Param("tenantId") String tenantId, @Param("status") Integer status);
}