package cn.aiedge.crm.quotation.mapper;

import cn.aiedge.crm.quotation.entity.Quotation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuotationMapper extends BaseMapper<Quotation> {

    @Select("SELECT * FROM crm_quotation WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY create_time DESC")
    List<Quotation> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM crm_quotation WHERE opportunity_id = #{opportunityId} AND deleted = 0 ORDER BY create_time DESC")
    List<Quotation> selectByOpportunityId(@Param("opportunityId") Long opportunityId);

    @Select("SELECT * FROM crm_quotation WHERE sales_person_id = #{salesPersonId} AND deleted = 0 ORDER BY create_time DESC")
    List<Quotation> selectBySalesPersonId(@Param("salesPersonId") Long salesPersonId);

    @Select("SELECT * FROM crm_quotation WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY version DESC")
    List<Quotation> selectVersionsByParentId(@Param("parentId") Long parentId);

    @Select("SELECT MAX(version) FROM crm_quotation WHERE parent_id = #{parentId} AND deleted = 0")
    Integer selectMaxVersionByParentId(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM crm_quotation WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM crm_quotation WHERE status IN (3, 4) AND valid_to < #{now} AND deleted = 0")
    List<Quotation> selectExpiredQuotations(@Param("now") java.time.LocalDate now);
}