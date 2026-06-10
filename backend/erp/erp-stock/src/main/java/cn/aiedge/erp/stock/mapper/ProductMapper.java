package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.Product;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

/**
 * 产品Mapper
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 分页查询产品(带分类名和等级名)
     */
    @Select("SELECT p.*, pc.category_name, pg.grade_name " +
            "FROM erp_product p " +
            "LEFT JOIN erp_product_category pc ON p.category_id = pc.id AND pc.deleted = 0 " +
            "LEFT JOIN erp_product_grade pg ON p.product_grade_id = pg.id AND pg.deleted = 0 " +
            "${ew.customSqlSegment}")
    @Results(id = "productWithRelation", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "category_name", property = "categoryName"),
            @Result(column = "grade_name", property = "gradeName")
    })
    IPage<Product> selectProductPage(IPage<Product> page, @Param(Constants.WRAPPER) Wrapper<Product> wrapper);

    /**
     * 查询产品详情(带关联信息)
     */
    @Select("SELECT p.*, pc.category_name, pg.grade_name " +
            "FROM erp_product p " +
            "LEFT JOIN erp_product_category pc ON p.category_id = pc.id AND pc.deleted = 0 " +
            "LEFT JOIN erp_product_grade pg ON p.product_grade_id = pg.id AND pg.deleted = 0 " +
            "WHERE p.id = #{id} AND p.deleted = 0")
    @Results({
            @Result(column = "category_name", property = "categoryName"),
            @Result(column = "grade_name", property = "gradeName")
    })
    Product selectProductDetail(@Param("id") Long id);
}
