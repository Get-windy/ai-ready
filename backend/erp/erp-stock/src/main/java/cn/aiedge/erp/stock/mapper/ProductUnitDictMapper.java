package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductUnitDict;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品单位字典Mapper
 */
@Mapper
public interface ProductUnitDictMapper extends BaseMapper<ProductUnitDict> {

    @Select("<script>" +
            "SELECT * FROM erp_product_unit_dict WHERE tenant_id = #{tenantId} AND deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND unit_name LIKE CONCAT('%', #{keyword}, '%')" +
            "</if>" +
            " ORDER BY sort_order ASC, id ASC" +
            "</script>")
    IPage<ProductUnitDict> selectPage(Page<ProductUnitDict> page,
                                       @Param("tenantId") Long tenantId,
                                       @Param("keyword") String keyword);

    @Select("SELECT * FROM erp_product_unit_dict WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<ProductUnitDict> selectByTenantId(@Param("tenantId") Long tenantId);
}
