package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.PromotionActivity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface PromotionActivityMapper extends BaseMapper<PromotionActivity> {

    /**
     * 查询当前生效的促销活动
     *
     * @param tenantId 租户ID
     * @param now 当前时间
     * @return 促销活动列表
     */
    @Select("SELECT * FROM erp_promotion_activity " +
            "WHERE deleted = 0 AND status = 'published' " +
            "AND tenant_id = #{tenantId} " +
            "AND start_time &lt;= #{now} AND end_time &gt;= #{now} " +
            "ORDER BY create_time DESC")
    List<PromotionActivity> selectActivePromotions(@Param("tenantId") Long tenantId,
                                                   @Param("now") LocalDateTime now);

    /**
     * 分页查询促销活动
     */
    Page<PromotionActivity> selectPageList(Page<PromotionActivity> page,
                                           @Param("tenantId") Long tenantId,
                                           @Param("name") String name,
                                           @Param("status") String status,
                                           @Param("type") String type);

    /**
     * 根据产品ID查询适用的促销
     */
    @Select("<script>" +
            "SELECT * FROM erp_promotion_activity " +
            "WHERE deleted = 0 AND status = 'published' " +
            "AND tenant_id = #{tenantId} " +
            "AND start_time &lt;= #{now} AND end_time &gt;= #{now} " +
            "AND (product_ids IS NULL OR product_ids = '' " +
            "     OR product_ids LIKE CONCAT('%', #{productId}, '%')) " +
            "<if test='customerLevel != null'> " +
            "  AND (customer_levels IS NULL OR customer_levels = '' " +
            "       OR customer_levels LIKE CONCAT('%', #{customerLevel}, '%')) " +
            "</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<PromotionActivity> selectApplicablePromotions(@Param("tenantId") Long tenantId,
                                                       @Param("productId") Long productId,
                                                       @Param("customerLevel") String customerLevel,
                                                       @Param("now") LocalDateTime now);
}
