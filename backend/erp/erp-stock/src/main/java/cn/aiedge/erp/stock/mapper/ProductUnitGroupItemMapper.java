package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductUnitGroupItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品单位组成员Mapper
 */
@Mapper
public interface ProductUnitGroupItemMapper extends BaseMapper<ProductUnitGroupItem> {

    @Select("SELECT * FROM erp_product_unit_group_item WHERE tenant_id = #{tenantId} AND deleted = 0 " +
            "ORDER BY sort_order ASC, id ASC")
    List<ProductUnitGroupItem> selectByTenantId(@Param("tenantId") Long tenantId);
}
