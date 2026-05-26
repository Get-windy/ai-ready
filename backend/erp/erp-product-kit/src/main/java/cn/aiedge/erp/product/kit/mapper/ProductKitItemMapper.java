package cn.aiedge.erp.product.kit.mapper;

import cn.aiedge.erp.product.kit.entity.ProductKitItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductKitItemMapper extends BaseMapper<ProductKitItem> {

    @Select("SELECT * FROM erp_product_kit_item WHERE kit_id = #{kitId} AND deleted = 0 ORDER BY line_no ASC")
    List<ProductKitItem> selectByKitId(@Param("kitId") Long kitId);

    @Select("SELECT SUM(line_cost) FROM erp_product_kit_item WHERE kit_id = #{kitId} AND deleted = 0")
    BigDecimal sumCostByKitId(@Param("kitId") Long kitId);

    @Select("SELECT COUNT(*) FROM erp_product_kit_item WHERE kit_id = #{kitId} AND deleted = 0")
    Integer countByKitId(@Param("kitId") Long kitId);
}