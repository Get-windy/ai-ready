package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductAttributeValue;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductAttributeValueMapper extends BaseMapper<ProductAttributeValue> {

    @Select("SELECT pav.*, pad.attr_name, pad.attr_type " +
            "FROM erp_product_attribute_value pav " +
            "LEFT JOIN erp_product_attribute_def pad ON pav.attr_def_id = pad.id AND pad.deleted = 0 " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(column = "attr_name", property = "attrName"),
            @Result(column = "attr_type", property = "attrType")
    })
    List<ProductAttributeValue> selectWithAttrDef(@Param(Constants.WRAPPER) Wrapper<ProductAttributeValue> wrapper);
}
