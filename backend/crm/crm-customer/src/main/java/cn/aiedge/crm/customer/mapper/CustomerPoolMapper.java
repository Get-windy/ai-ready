package cn.aiedge.crm.customer.mapper;

import cn.aiedge.crm.customer.entity.CustomerPool;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomerPoolMapper extends BaseMapper<CustomerPool> {

    @Select("SELECT * FROM crm_customer_pool WHERE status = 1 AND deleted = 0 ORDER BY pool_time DESC")
    List<CustomerPool> selectAvailable();

    @Select("SELECT * FROM crm_customer_pool WHERE claim_sales_person_id = #{salesPersonId} AND deleted = 0 ORDER BY claim_time DESC")
    List<CustomerPool> selectByClaimSalesPerson(@Param("salesPersonId") Long salesPersonId);

    @Select("SELECT * FROM crm_customer_pool WHERE original_sales_person_id = #{salesPersonId} AND deleted = 0 ORDER BY pool_time DESC")
    List<CustomerPool> selectByOriginalSalesPerson(@Param("salesPersonId") Long salesPersonId);

    @Select("SELECT * FROM crm_customer_pool WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY pool_time DESC LIMIT 1")
    CustomerPool selectLatestByCustomer(@Param("customerId") Long customerId);

    @Select("SELECT COUNT(*) FROM crm_customer_pool WHERE status = 1 AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countAvailable(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM crm_customer_pool WHERE claim_sales_person_id = #{salesPersonId} AND deleted = 0")
    Integer countByClaimSalesPerson(@Param("salesPersonId") Long salesPersonId);
}