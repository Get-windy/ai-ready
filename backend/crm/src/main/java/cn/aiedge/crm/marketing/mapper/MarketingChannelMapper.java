package cn.aiedge.crm.marketing.mapper;

import cn.aiedge.crm.marketing.entity.MarketingChannel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarketingChannelMapper extends BaseMapper<MarketingChannel> {

    @Select("SELECT * FROM crm_marketing_channel WHERE active = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<MarketingChannel> selectActiveChannels();

    @Select("SELECT * FROM crm_marketing_channel WHERE channel_type = #{channelType} AND active = 1 AND deleted = 0")
    List<MarketingChannel> selectByChannelType(@Param("channelType") Integer channelType);
}