package cn.aiedge.crm.quotation.mapper;

import cn.aiedge.crm.quotation.entity.QuotationItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuotationItemMapper extends BaseMapper<QuotationItem> {

    @Select("SELECT * FROM crm_quotation_item WHERE quotation_id = #{quotationId} AND deleted = 0 ORDER BY line_no ASC")
    List<QuotationItem> selectByQuotationId(@Param("quotationId") Long quotationId);

    @Select("SELECT SUM(line_total) FROM crm_quotation_item WHERE quotation_id = #{quotationId} AND deleted = 0")
    java.math.BigDecimal sumLineTotalByQuotationId(@Param("quotationId") Long quotationId);

    @Select("SELECT COUNT(*) FROM crm_quotation_item WHERE quotation_id = #{quotationId} AND deleted = 0")
    Integer countByQuotationId(@Param("quotationId") Long quotationId);
}