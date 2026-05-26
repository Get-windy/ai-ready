package cn.aiedge.crm.contract.mapper;

import cn.aiedge.crm.contract.entity.Contract;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    @Select("SELECT * FROM crm_contract WHERE customer_id = #{customerId} AND deleted = 0 ORDER BY create_time DESC")
    List<Contract> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM crm_contract WHERE opportunity_id = #{opportunityId} AND deleted = 0 ORDER BY create_time DESC")
    List<Contract> selectByOpportunityId(@Param("opportunityId") Long opportunityId);

    @Select("SELECT * FROM crm_contract WHERE sales_person_id = #{salesPersonId} AND deleted = 0 ORDER BY create_time DESC")
    List<Contract> selectBySalesPersonId(@Param("salesPersonId") Long salesPersonId);

    @Select("SELECT * FROM crm_contract WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<Contract> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM crm_contract WHERE end_date BETWEEN #{startDate} AND #{endDate} AND status IN (5, 6) AND deleted = 0")
    List<Contract> selectExpiringContracts(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Select("SELECT * FROM crm_contract WHERE end_date < #{now} AND status IN (5, 6) AND deleted = 0")
    List<Contract> selectExpiredContracts(@Param("now") java.time.LocalDate now);

    @Select("SELECT COUNT(*) FROM crm_contract WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(contract_amount) FROM crm_contract WHERE status IN (5, 6, 7) AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumEffectiveContractAmount(@Param("tenantId") Long tenantId);
}