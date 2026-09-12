package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.MallTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 商城标签Mapper
 */
@Mapper
public interface MallTagMapper extends BaseMapper<MallTag> {

    /**
     * 查询租户下的所有标签（按排序）
     */
    @Select("SELECT * FROM erp_mall_tag WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<MallTag> selectByTenantId(Long tenantId);

    /**
     * 分页查询标签（关键词匹配标签名称）
     */
    @Select("<script>" +
            "SELECT * FROM erp_mall_tag WHERE tenant_id = #{tenantId} AND deleted = 0" +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND tag_name LIKE CONCAT('%', #{keyword}, '%')" +
            "</if>" +
            " ORDER BY sort_order ASC, id ASC" +
            "</script>")
    IPage<MallTag> selectPage(Page<MallTag> page,
                              @Param("tenantId") Long tenantId,
                              @Param("keyword") String keyword);

    /**
     * 查询打了标签的商品（商品名 + 标签串），用于「对应商品」聚合。
     * 仅取有标签的商品，避免全表扫描结果集过大。
     *
     * <p>⚠️ 别名必须加双引号：PostgreSQL 会把未加引号的别名折叠成小写（productName → productname），
     * 导致 Java 侧 {@code row.get("productName")} 恒为 null、「对应商品」列始终为空。</p>
     */
    @Select("SELECT product_name AS \"productName\", mall_tags AS \"mallTags\" FROM erp_product " +
            "WHERE tenant_id = #{tenantId} AND deleted = 0 " +
            "AND mall_tags IS NOT NULL AND mall_tags <> ''")
    List<Map<String, Object>> selectTaggedProducts(@Param("tenantId") Long tenantId);
}
