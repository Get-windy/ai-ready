package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductBrand;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品品牌Mapper
 */
@Mapper
public interface ProductBrandMapper extends BaseMapper<ProductBrand> {

    /**
     * 分页查询品牌列表
     */
    @Select("<script>" +
            "SELECT * FROM erp_product_brand WHERE tenant_id = #{tenantId} AND deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (brand_name LIKE CONCAT('%', #{keyword}, '%') OR mnemonic_code LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            " ORDER BY sort_order ASC, id ASC" +
            "</script>")
    IPage<ProductBrand> selectPage(Page<ProductBrand> page,
                                    @Param("tenantId") Long tenantId,
                                    @Param("keyword") String keyword);

    /**
     * 查询租户下所有品牌
     */
    @Select("SELECT * FROM erp_product_brand WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<ProductBrand> selectByTenantId(@Param("tenantId") Long tenantId);
}
