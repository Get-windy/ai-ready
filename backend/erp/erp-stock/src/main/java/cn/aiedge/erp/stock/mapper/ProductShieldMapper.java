package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductShield;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商品授权（屏蔽客户）Mapper
 */
@Mapper
public interface ProductShieldMapper extends BaseMapper<ProductShield> {

    /**
     * 分页查询授权（关联商品主数据展示图片/名称/货号等列）
     */
    @Select("<script>" +
            "SELECT s.*, p.product_name, p.product_code_alias, p.image_url, p.barcode, " +
            "p.spec, p.model, p.origin, p.brand " +
            "FROM erp_product_shield s " +
            "LEFT JOIN erp_product p ON p.id = s.product_id AND p.deleted = 0 " +
            "WHERE s.deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (p.product_name LIKE CONCAT('%', #{keyword}, '%')" +
            "      OR p.product_code_alias LIKE CONCAT('%', #{keyword}, '%')" +
            "      OR p.barcode LIKE CONCAT('%', #{keyword}, '%')" +
            "      OR s.partner_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='shieldLevel != null and shieldLevel != \"\"'>" +
            " AND s.shield_level = #{shieldLevel}" +
            "</if>" +
            "<if test='region != null and region != \"\"'>" +
            " AND s.region LIKE CONCAT('%', #{region}, '%')" +
            "</if>" +
            "<if test='partnerId != null'>" +
            " AND s.partner_id = #{partnerId}" +
            "</if>" +
            " ORDER BY s.create_time DESC" +
            "</script>")
    IPage<ProductShield> selectShieldPage(IPage<ProductShield> page,
                                          @Param("keyword") String keyword,
                                          @Param("shieldLevel") String shieldLevel,
                                          @Param("region") String region,
                                          @Param("partnerId") Long partnerId);
}
