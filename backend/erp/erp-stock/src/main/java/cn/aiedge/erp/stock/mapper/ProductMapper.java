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
     * 分页查询产品(带分类名/等级名/可用库存)
     */
    @Select("<script>" +
            "SELECT p.*, pc.category_name, pg.grade_name, " +
            "COALESCE(st.available_stock, 0) AS available_stock, st.default_warehouse_name " +
            "FROM erp_product p " +
            "LEFT JOIN erp_product_category pc ON p.category_id = pc.id AND pc.deleted = 0 " +
            "LEFT JOIN erp_product_grade pg ON p.product_grade_id = pg.id AND pg.deleted = 0 " +
            "LEFT JOIN (SELECT product_id, SUM(COALESCE(available_quantity, 0)) AS available_stock, " +
            "           (ARRAY_AGG(warehouse_name ORDER BY COALESCE(available_quantity, 0) DESC))[1] AS default_warehouse_name " +
            "           FROM erp_stock WHERE deleted = 0 GROUP BY product_id) st ON st.product_id = p.id " +
            "WHERE p.deleted = 0 " +
            "<if test='ew != null and ew.sqlSegment != null and ew.sqlSegment != \"\"'>" +
            "AND ${ew.sqlSegment}" +
            "</if>" +
            "ORDER BY p.create_time DESC" +
            "</script>")
    @Results(id = "productWithRelation", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "category_name", property = "categoryName"),
            @Result(column = "grade_name", property = "gradeName"),
            @Result(column = "available_stock", property = "availableStock"),
            @Result(column = "default_warehouse_name", property = "defaultWarehouseName")
    })
    IPage<Product> selectProductPage(IPage<Product> page, @Param(Constants.WRAPPER) Wrapper<Product> wrapper);

    /**
     * 查询产品详情(带关联信息)
     */
    @Select("SELECT p.*, pc.category_name, pg.grade_name, " +
            "COALESCE(st.available_stock, 0) AS available_stock " +
            "FROM erp_product p " +
            "LEFT JOIN erp_product_category pc ON p.category_id = pc.id AND pc.deleted = 0 " +
            "LEFT JOIN erp_product_grade pg ON p.product_grade_id = pg.id AND pg.deleted = 0 " +
            "LEFT JOIN (SELECT product_id, SUM(COALESCE(available_quantity, 0)) AS available_stock " +
            "           FROM erp_stock WHERE deleted = 0 GROUP BY product_id) st ON st.product_id = p.id " +
            "WHERE p.id = #{id} AND p.deleted = 0")
    @Results({
            @Result(column = "category_name", property = "categoryName"),
            @Result(column = "grade_name", property = "gradeName"),
            @Result(column = "available_stock", property = "availableStock")
    })
    Product selectProductDetail(@Param("id") Long id);
}
