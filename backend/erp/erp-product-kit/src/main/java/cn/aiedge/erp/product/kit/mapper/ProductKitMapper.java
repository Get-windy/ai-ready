package cn.aiedge.erp.product.kit.mapper;

import cn.aiedge.erp.product.kit.entity.ProductKit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductKitMapper extends BaseMapper<ProductKit> {

    @Select("SELECT * FROM erp_product_kit WHERE product_id = #{productId} AND deleted = 0")
    List<ProductKit> selectByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM erp_product_kit WHERE kit_type = #{kitType} AND active = 1 AND deleted = 0")
    List<ProductKit> selectByKitType(@Param("kitType") Integer kitType);

    @Select("SELECT * FROM erp_product_kit WHERE active = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<ProductKit> selectActiveKits();

    @Select("SELECT COUNT(*) FROM erp_product_kit WHERE active = 1 AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countActiveKits(@Param("tenantId") Long tenantId);
}