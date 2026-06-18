package cn.aiedge.crm.marketing.mapper;

import cn.aiedge.crm.marketing.entity.MarketingExecution;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarketingExecutionMapper extends BaseMapper<MarketingExecution> {

    @Select("SELECT * FROM crm_marketing_execution WHERE campaign_id = #{campaignId} AND deleted = 0 ORDER BY execution_time DESC")
    List<MarketingExecution> selectByCampaignId(@Param("campaignId") Long campaignId);

    @Select("SELECT * FROM crm_marketing_execution WHERE target_id = #{targetId} AND deleted = 0 ORDER BY execution_time DESC")
    List<MarketingExecution> selectByTargetId(@Param("targetId") Long targetId);

    @Select("SELECT * FROM crm_marketing_execution WHERE campaign_id = #{campaignId} AND execution_channel = #{channel} AND deleted = 0")
    List<MarketingExecution> selectByCampaignIdAndChannel(@Param("campaignId") Long campaignId, @Param("channel") Integer channel);

    @Select("SELECT SUM(cost) FROM crm_marketing_execution WHERE campaign_id = #{campaignId} AND deleted = 0")
    java.math.BigDecimal sumCostByCampaignId(@Param("campaignId") Long campaignId);

    @Select("SELECT COUNT(*) FROM crm_marketing_execution WHERE campaign_id = #{campaignId} AND execution_result = #{result} AND deleted = 0")
    Integer countByCampaignIdAndResult(@Param("campaignId") Long campaignId, @Param("result") Integer result);
}