package cn.aiedge.crm.quotation.mapper;

import cn.aiedge.crm.quotation.entity.QuotationTemplateItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuotationTemplateItemMapper extends BaseMapper<QuotationTemplateItem> {

    @Select("SELECT * FROM crm_quotation_template_item WHERE template_id = #{templateId} AND deleted = 0 ORDER BY line_no ASC")
    List<QuotationTemplateItem> selectByTemplateId(@Param("templateId") Long templateId);
}