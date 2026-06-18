package cn.aiedge.crm.marketing.mapper;

import cn.aiedge.crm.marketing.entity.MarketingCampaign;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarketingCampaignMapper extends BaseMapper<MarketingCampaign> {

    @Select("SELECT * FROM crm_marketing_campaign WHERE owner_id = #{ownerId} AND deleted = 0 ORDER BY create_time DESC")
    List<MarketingCampaign> selectByOwnerId(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM crm_marketing_campaign WHERE department_id = #{departmentId} AND deleted = 0 ORDER BY create_time DESC")
    List<MarketingCampaign> selectByDepartmentId(@Param("departmentId") Long departmentId);

    @Select("SELECT * FROM crm_marketing_campaign WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<MarketingCampaign> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM crm_marketing_campaign WHERE start_date <= #{now} AND end_date >= #{now} AND status = 4 AND deleted = 0")
    List<MarketingCampaign> selectRunningCampaigns(@Param("now") java.time.LocalDate now);

    @Select("SELECT * FROM crm_marketing_campaign WHERE end_date < #{now} AND status = 4 AND deleted = 0")
    List<MarketingCampaign> selectEndedCampaigns(@Param("now") java.time.LocalDate now);

    @Select("SELECT COUNT(*) FROM crm_marketing_campaign WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT SUM(budget) FROM crm_marketing_campaign WHERE status IN (2, 3, 4, 6) AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumBudget(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(actual_cost) FROM crm_marketing_campaign WHERE status = 6 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumActualCost(@Param("tenantId") Long tenantId);

    @Select("SELECT SUM(actual_revenue) FROM crm_marketing_campaign WHERE status = 6 AND deleted = 0 AND tenant_id = #{tenantId}")
    java.math.BigDecimal sumActualRevenue(@Param("tenantId") Long tenantId);
}