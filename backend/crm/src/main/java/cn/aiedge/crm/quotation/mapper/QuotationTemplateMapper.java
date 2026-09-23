package cn.aiedge.crm.quotation.mapper;

import cn.aiedge.crm.quotation.entity.QuotationTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuotationTemplateMapper extends BaseMapper<QuotationTemplate> {

    // ⚠️ `crm_quotation_template.active` 是 boolean 列，写成 `= 1` 会报
    //    「操作符不存在: boolean = integer」→ 三个端点全部 500（2026-09-23 真机实测）
    @Select("SELECT * FROM crm_quotation_template WHERE active = TRUE AND deleted = 0 ORDER BY usage_count DESC")
    List<QuotationTemplate> selectActiveTemplates();

    @Select("SELECT * FROM crm_quotation_template WHERE customer_id = #{customerId} AND active = TRUE AND deleted = 0")
    List<QuotationTemplate> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM crm_quotation_template WHERE product_category_id = #{categoryId} AND active = TRUE AND deleted = 0")
    List<QuotationTemplate> selectByCategoryId(@Param("categoryId") Long categoryId);
}