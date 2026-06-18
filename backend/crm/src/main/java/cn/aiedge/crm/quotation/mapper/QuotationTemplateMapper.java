package cn.aiedge.crm.quotation.mapper;

import cn.aiedge.crm.quotation.entity.QuotationTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuotationTemplateMapper extends BaseMapper<QuotationTemplate> {

    @Select("SELECT * FROM crm_quotation_template WHERE active = 1 AND deleted = 0 ORDER BY usage_count DESC")
    List<QuotationTemplate> selectActiveTemplates();

    @Select("SELECT * FROM crm_quotation_template WHERE customer_id = #{customerId} AND active = 1 AND deleted = 0")
    List<QuotationTemplate> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM crm_quotation_template WHERE product_category_id = #{categoryId} AND active = 1 AND deleted = 0")
    List<QuotationTemplate> selectByCategoryId(@Param("categoryId") Long categoryId);
}