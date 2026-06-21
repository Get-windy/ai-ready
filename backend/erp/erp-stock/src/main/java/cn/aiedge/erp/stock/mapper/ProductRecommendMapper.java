package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductRecommend;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 推荐商品Mapper
 */
public interface ProductRecommendMapper extends BaseMapper<ProductRecommend> {

    /**
     * 查询商品的推荐列表(带关联产品信息)
     */
    @Select("SELECT pr.*, " +
            "p2.product_code AS recommend_product_code, " +
            "p2.product_name AS recommend_product_name, " +
            "p2.spec AS recommend_product_spec, " +
            "p2.unit AS recommend_product_unit, " +
            "p2.origin AS recommend_product_origin, " +
            "p2.brand AS recommend_product_brand " +
            "FROM erp_product_recommend pr " +
            "LEFT JOIN erp_product p2 ON pr.recommend_product_id = p2.id AND p2.deleted = 0 " +
            "WHERE pr.product_id = #{productId} AND pr.deleted = 0 " +
            "ORDER BY pr.sort_order ASC")
    @Results({
            @Result(column = "recommend_product_code", property = "recommendProductCode"),
            @Result(column = "recommend_product_name", property = "recommendProductName"),
            @Result(column = "recommend_product_spec", property = "recommendProductSpec"),
            @Result(column = "recommend_product_unit", property = "recommendProductUnit"),
            @Result(column = "recommend_product_origin", property = "recommendProductOrigin"),
            @Result(column = "recommend_product_brand", property = "recommendProductBrand")
    })
    List<ProductRecommend> selectByProductId(@Param("productId") Long productId);
}
