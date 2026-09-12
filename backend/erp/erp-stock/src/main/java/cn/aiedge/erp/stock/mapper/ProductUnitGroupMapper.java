package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductUnitGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品单位组Mapper
 *
 * <p>对标「单位组管理」查询条件为「单位」关键词，单位名存在明细表，
 * 故关键词走 EXISTS 子查询匹配组内单位名。
 */
@Mapper
public interface ProductUnitGroupMapper extends BaseMapper<ProductUnitGroup> {

    @Select("<script>" +
            "SELECT g.* FROM erp_product_unit_group g WHERE g.tenant_id = #{tenantId} AND g.deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND EXISTS (SELECT 1 FROM erp_product_unit_group_item i" +
            " WHERE i.group_id = g.id AND i.deleted = 0 AND i.unit_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='status != null'>" +
            " AND g.status = #{status}" +
            "</if>" +
            " ORDER BY g.id DESC" +
            "</script>")
    IPage<ProductUnitGroup> selectPage(Page<ProductUnitGroup> page,
                                       @Param("tenantId") Long tenantId,
                                       @Param("keyword") String keyword,
                                       @Param("status") Integer status);

    @Select("SELECT * FROM erp_product_unit_group WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY id DESC")
    List<ProductUnitGroup> selectByTenantId(@Param("tenantId") Long tenantId);
}
