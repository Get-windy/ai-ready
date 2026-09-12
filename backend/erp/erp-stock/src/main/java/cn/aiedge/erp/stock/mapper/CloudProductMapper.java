package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.CloudProduct;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 云商品库 Mapper
 */
@Mapper
public interface CloudProductMapper extends BaseMapper<CloudProduct> {

    @Select("<script>" +
            "SELECT * FROM erp_product_cloud_catalog WHERE deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (cloud_name LIKE CONCAT('%', #{keyword}, '%')" +
            "      OR cloud_code LIKE CONCAT('%', #{keyword}, '%')" +
            "      OR barcode LIKE CONCAT('%', #{keyword}, '%')" +
            "      OR brand LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='industryCategory != null and industryCategory != \"\"'>" +
            " AND industry_category = #{industryCategory}" +
            "</if>" +
            " ORDER BY id ASC" +
            "</script>")
    IPage<CloudProduct> selectCloudPage(Page<CloudProduct> page,
                                        @Param("keyword") String keyword,
                                        @Param("industryCategory") String industryCategory);
}
