package cn.aiedge.erp.marketing.mapper;

import cn.aiedge.erp.marketing.dto.PointsExchangeRow;
import cn.aiedge.erp.marketing.entity.PointsExchangeProduct;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsExchangeProductMapper extends BaseMapper<PointsExchangeProduct> {

    /**
     * 积分兑换目录分页（对标 13 列）。
     * 商品名称/货号/单位/规格/型号/产地与 6 个价格列取自商品主数据（价格取基本单位行）。
     */
    @Select("""
            <script>
            SELECT e.id                                    AS id,
                   e.product_id                            AS product_id,
                   p.product_name                           AS product_name,
                   p.product_code                           AS product_code,
                   p.unit                                   AS unit,
                   e.exchange_points                        AS exchange_points,
                   p.spec                                   AS spec,
                   p.model                                  AS model,
                   p.origin                                 AS origin,
                   pu.preset_purchase_price                 AS preset_purchase_price,
                   pu.reference_cost                        AS reference_cost,
                   pu.recent_purchase_price                 AS recent_purchase_price,
                   pu.wholesale_price                       AS wholesale_price,
                   pu.retail_price                          AS retail_price,
                   pu.min_sale_price                        AS min_sale_price,
                   e.status                                 AS status,
                   e.sort                                   AS sort,
                   e.remark                                 AS remark
            FROM mkt_points_exchange_product e
            JOIN erp_product p ON p.id = e.product_id AND p.deleted = 0
            LEFT JOIN erp_product_unit pu
                   ON pu.product_id = p.id AND pu.deleted = 0 AND pu.is_base_unit = 1
            WHERE e.deleted = 0
              AND e.tenant_id = #{tenantId}
              <if test="keyword != null and keyword != ''">
                AND (p.product_code LIKE CONCAT('%', #{keyword}, '%')
                     OR p.product_name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY e.sort ASC NULLS LAST, e.id DESC
            </script>
            """)
    IPage<PointsExchangeRow> selectExchangePage(Page<PointsExchangeRow> page,
                                                @Param("tenantId") Long tenantId,
                                                @Param("keyword") String keyword);
}
