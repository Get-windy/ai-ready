package cn.aiedge.crm.marketing.mapper;

import cn.aiedge.crm.marketing.entity.MarketingTarget;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarketingTargetMapper extends BaseMapper<MarketingTarget> {

    @Select("SELECT * FROM crm_marketing_target WHERE campaign_id = #{campaignId} AND deleted = 0 ORDER BY target_priority DESC")
    List<MarketingTarget> selectByCampaignId(@Param("campaignId") Long campaignId);

    @Select("SELECT * FROM crm_marketing_target WHERE campaign_id = #{campaignId} AND target_status = #{status} AND deleted = 0")
    List<MarketingTarget> selectByCampaignIdAndStatus(@Param("campaignId") Long campaignId, @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM crm_marketing_target WHERE campaign_id = #{campaignId} AND deleted = 0")
    Integer countByCampaignId(@Param("campaignId") Long campaignId);

    @Select("SELECT COUNT(*) FROM crm_marketing_target WHERE campaign_id = #{campaignId} AND reach_status = 1 AND deleted = 0")
    Integer countReachedByCampaignId(@Param("campaignId") Long campaignId);

    @Select("SELECT COUNT(*) FROM crm_marketing_target WHERE campaign_id = #{campaignId} AND response_status = 1 AND deleted = 0")
    Integer countRespondedByCampaignId(@Param("campaignId") Long campaignId);

    @Select("SELECT COUNT(*) FROM crm_marketing_target WHERE campaign_id = #{campaignId} AND conversion_status = 1 AND deleted = 0")
    Integer countConvertedByCampaignId(@Param("campaignId") Long campaignId);

    @Select("SELECT * FROM crm_marketing_target WHERE campaign_id = #{campaignId} AND customer_id = #{customerId} AND deleted = 0")
    MarketingTarget selectByCampaignIdAndCustomerId(@Param("campaignId") Long campaignId, @Param("customerId") Long customerId);
}