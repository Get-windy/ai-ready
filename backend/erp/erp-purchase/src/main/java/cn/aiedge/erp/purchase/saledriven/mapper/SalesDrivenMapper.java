package cn.aiedge.erp.purchase.saledriven.mapper;

import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenItemDTO;
import cn.aiedge.erp.purchase.saledriven.dto.SalesDrivenRowDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 以销定购 Mapper
 * <p>
 * 本模块位于 erp-purchase，跨模块只读访问销售订单表（erp_sale_order / erp_sale_order_item /
 * erp_sale_order_deposit）与商品/BOM 表（erp_product / erp_product_kit / erp_product_kit_item），
 * 只读不写，创建采购订单复用 {@code PurchaseOrderService}。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SalesDrivenMapper {

    /**
     * 以销定购-销售订单列表分页（42列 + 订金前两条 + 是否已采购）
     *
     * @param page    分页参数
     * @param wrapper 查询条件（列名带表别名前缀，如 o.order_no, o.order_date）
     * @return 分页结果
     */
    Page<SalesDrivenRowDTO> selectPageWithDetails(Page<?> page, @Param("ew") Wrapper<SalesDrivenRowDTO> wrapper);

    /**
     * 查询销售订单明细（用于采购成品/采购原料）
     *
     * @param orderId 销售订单ID
     * @return 明细列表
     */
    List<SalesDrivenItemDTO> selectItemsByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询指定商品默认供应商（最近一次采购订单的供应商）
     *
     * @param productId 商品ID
     * @return 供应商ID，无历史采购时返回 null
     */
    @Select("SELECT po.supplier_id " +
            "FROM erp_purchase_order_item poi " +
            "INNER JOIN erp_purchase_order po ON po.id = poi.order_id AND po.deleted = 0 " +
            "WHERE poi.product_id = #{productId} " +
            "ORDER BY po.order_date DESC, po.id DESC LIMIT 1")
    Long selectDefaultSupplierId(@Param("productId") Long productId);

    /**
     * 查询指定商品默认供应商名称（最近一次采购订单的供应商快照名）
     *
     * @param productId 商品ID
     * @return 供应商名称
     */
    @Select("SELECT ps.supplier_name " +
            "FROM erp_purchase_order_item poi " +
            "INNER JOIN erp_purchase_order po ON po.id = poi.order_id AND po.deleted = 0 " +
            "LEFT JOIN erp_purchase_order_partner_snapshot ps ON ps.order_id = po.id " +
            "WHERE poi.product_id = #{productId} " +
            "ORDER BY po.order_date DESC, po.id DESC LIMIT 1")
    String selectDefaultSupplierName(@Param("productId") Long productId);

    /**
     * 查询指定商品采购价（erp_product.purchase_price）
     *
     * @param productId 商品ID
     * @return 采购价
     */
    @Select("SELECT purchase_price FROM erp_product WHERE id = #{productId} AND deleted = 0")
    BigDecimal selectProductPurchasePrice(@Param("productId") Long productId);

    /**
     * 查询商品对应 BOM 成品（active 且未删除，按 product_id 匹配）
     *
     * @param productId 成品商品ID
     * @return 套装ID（erp_product_kit.id）
     */
    Long selectActiveKitIdByProductId(@Param("productId") Long productId);

    /**
     * 查询 BOM 原料明细（component_product_id/名称/规格/单位/单耗数量）
     *
     * @param kitId 套装ID
     * @return 原料明细
     */
    List<SalesDrivenItemDTO> selectKitComponents(@Param("kitId") Long kitId);

    /**
     * 查询销售订单编号
     *
     * @param orderId 销售订单ID
     * @return 单据编号
     */
    String selectOrderNoById(@Param("orderId") Long orderId);

    /**
     * 查询销售发货仓库ID
     *
     * @param orderId 销售订单ID
     * @return 仓库ID
     */
    Long selectWarehouseIdById(@Param("orderId") Long orderId);

    /**
     * 按源单号统计未完成采购单数量（用于重复采购校验）
     *
     * @param sourceBillNo 源单编号
     * @return 在途采购单数
     */
    int countActivePurchaseBySourceBillNo(@Param("sourceBillNo") String sourceBillNo);
}
