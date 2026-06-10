package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductRelated;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductRelatedMapper extends BaseMapper<ProductRelated> {

    @Select("SELECT pr.*, p.product_code AS related_product_code, " +
            "p.product_name AS related_product_name, p.spec AS related_product_spec " +
            "FROM erp_product_related pr " +
            "LEFT JOIN erp_product p ON pr.related_product_id = p.id AND p.deleted = 0 " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(column = "related_product_code", property = "relatedProductCode"),
            @Result(column = "related_product_name", property = "relatedProductName"),
            @Result(column = "related_product_spec", property = "relatedProductSpec")
    })
    List<ProductRelated> selectWithProduct(@Param(Constants.WRAPPER) Wrapper<ProductRelated> wrapper);
}
