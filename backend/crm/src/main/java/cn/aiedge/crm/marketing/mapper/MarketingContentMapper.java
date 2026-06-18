package cn.aiedge.crm.marketing.mapper;

import cn.aiedge.crm.marketing.entity.MarketingContent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MarketingContentMapper extends BaseMapper<MarketingContent> {

    @Select("SELECT * FROM crm_marketing_content WHERE active = 1 AND deleted = 0 ORDER BY usage_count DESC")
    List<MarketingContent> selectActiveContents();

    @Select("SELECT * FROM crm_marketing_content WHERE content_type = #{contentType} AND active = 1 AND deleted = 0")
    List<MarketingContent> selectByContentType(@Param("contentType") Integer contentType);

    @Select("SELECT * FROM crm_marketing_content WHERE content_category = #{category} AND active = 1 AND deleted = 0")
    List<MarketingContent> selectByContentCategory(@Param("category") Integer category);
}