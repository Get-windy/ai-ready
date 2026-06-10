package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductGradePrice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 产品等级价格Mapper
 */
public interface ProductGradePriceMapper extends BaseMapper<ProductGradePrice> {

    /**
     * 查询某产品的等级价格(含等级名称)
     */
    @Select("SELECT pgp.*, pg.grade_name, pg.grade_code " +
            "FROM erp_product_grade_price pgp " +
            "LEFT JOIN erp_product_grade pg ON pgp.product_grade_id = pg.id " +
            "WHERE pgp.product_id = #{productId} AND pgp.deleted = 0 AND pg.deleted = 0 " +
            "ORDER BY pg.grade_level DESC")
    List<ProductGradePrice> selectByProductIdWithGrade(Long productId);
}
