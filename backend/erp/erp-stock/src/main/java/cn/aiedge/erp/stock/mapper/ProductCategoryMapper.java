package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 产品分类Mapper
 */
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {

    /**
     * 获取完整的分类树(按排序)
     */
    @Select("SELECT * FROM erp_product_category WHERE deleted = 0 ORDER BY parent_id, sort_order")
    List<ProductCategory> selectAllOrdered();

    /**
     * 统计某分类下的产品数量
     */
    @Select("SELECT COUNT(*) FROM erp_product WHERE category_id = #{categoryId} AND deleted = 0")
    Integer countProductByCategory(Long categoryId);
}
