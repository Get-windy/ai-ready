package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.dto.PurchasePriceTrendVO;
import cn.aiedge.erp.purchase.entity.PurchasePriceTrack;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 采购价格跟踪Mapper
 * <p>
 * 列表：按 product_id + partner_id 分组取"最近一条"采购价；支持商品名/供应商名/日期/商品分类过滤。
 * 趋势：按 product_id 取全部价格点，按采购日期升序。
 * 主数据补全：新增"价格折扣"时按商品ID/往来单位ID 选取基础信息快照。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface PurchasePriceTrackMapper extends BaseMapper<PurchasePriceTrack> {

    /**
     * 分页查询价格跟踪列表（每组 product×partner 取最近一条）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
        + "SELECT x.id, x.product_id, x.product_code, x.product_name, x.item_code, x.unit,"
        + "       x.specification, x.model, x.origin, x.barcode,"
        + "       x.partner_id, x.partner_code, x.partner_name,"
        + "       x.purchase_price, x.purchase_date, x.last_modify_time, x.source"
        + " FROM ("
        + "   SELECT t.*,"
        + "          ROW_NUMBER() OVER (PARTITION BY t.product_id, t.partner_id"
        + "            ORDER BY t.purchase_date DESC NULLS LAST, t.update_time DESC, t.id DESC) AS rn"
        + "   FROM erp_purchase_price_track t"
        + "   WHERE t.deleted = 0"
        + "   <if test=\"productName != null and productName != ''\">"
        + "     AND t.product_name ILIKE CONCAT('%', #{productName}, '%')"
        + "   </if>"
        + "   <if test=\"partnerName != null and partnerName != ''\">"
        + "     AND t.partner_name ILIKE CONCAT('%', #{partnerName}, '%')"
        + "   </if>"
        + "   <if test=\"startDate != null and startDate != ''\">"
        + "     AND t.purchase_date &gt;= CAST(#{startDate} AS DATE)"
        + "   </if>"
        + "   <if test=\"endDate != null and endDate != ''\">"
        + "     AND t.purchase_date &lt;= CAST(#{endDate} AS DATE)"
        + "   </if>"
        + "   <if test=\"unitType != null and unitType != ''\">"
        + "     AND t.unit = #{unitType}"
        + "   </if>"
        + "   <if test=\"categoryId != null\">"
        + "     AND EXISTS (SELECT 1 FROM erp_product p WHERE p.id = t.product_id"
        + "       AND p.category_id = #{categoryId})"
        + "   </if>"
        + " ) x"
        + " WHERE x.rn = 1"
        + " ORDER BY x.last_modify_time DESC NULLS LAST, x.purchase_date DESC"
        + "</script>")
    IPage<PurchasePriceTrack> selectTrackPage(Page<PurchasePriceTrack> page,
                                              @Param("productName") String productName,
                                              @Param("partnerName") String partnerName,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate,
                                              @Param("categoryId") Long categoryId,
                                              @Param("unitType") String unitType);

    /**
     * 趋势：某商品全部价格点（按采购日期升序）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, product_id, product_name, purchase_date, purchase_price, partner_name"
        + " FROM erp_purchase_price_track"
        + " WHERE deleted = 0 AND product_id = #{productId}"
        + " ORDER BY purchase_date ASC NULLS LAST, id ASC")
    List<PurchasePriceTrendVO> selectTrend(@Param("productId") Long productId);

    /**
     * 商品主数据快照（新增/修改时回填商品基础信息）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, product_code, product_name,"
        + " COALESCE(NULLIF(product_code_alias, ''), product_code) AS item_code,"
        + " unit, spec AS specification, model, origin, barcode"
        + " FROM erp_product WHERE id = #{productId} AND deleted = 0")
    Map<String, Object> selectProductSnapshot(@Param("productId") Long productId);

    /**
     * 往来单位主数据快照（新增/修改时回填往来单位编号/名称）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, party_code AS partner_code, party_name AS partner_name"
        + " FROM biz_party WHERE id = #{partnerId} AND deleted = 0")
    Map<String, Object> selectPartnerSnapshot(@Param("partnerId") Long partnerId);
}
