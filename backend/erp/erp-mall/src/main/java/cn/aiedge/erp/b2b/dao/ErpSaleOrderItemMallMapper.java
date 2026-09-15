package cn.aiedge.erp.b2b.dao;

import cn.aiedge.erp.b2b.dto.MallOrderItemPageDTO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * erp_sale_order_item 表 Mapper
 *
 * <p>除基础 CRUD 外，提供商城管理端「按明细」分页（明细 JOIN 单头），
 * 与 SalePreOrderItemMapper#selectDetailPage 的成熟写法保持一致。</p>
 */
@Mapper
public interface ErpSaleOrderItemMallMapper extends BaseMapper<ErpSaleOrderItemMall> {

    /**
     * 商城订单「按明细」分页：erp_sale_order_item JOIN erp_sale_order
     *
     * <p>tenant_id 由 MyBatis-Plus TenantLineInnerInterceptor 自动注入（对 JOIN 的每张表分别注入），
     * 因此本 SQL 不再手工拼接租户条件。</p>
     *
     * <p>说明：仅按单头 {@code o.deleted = 0} 过滤（明细表在部分历史环境中无 deleted 列，
     * 故不对明细做逻辑删除过滤，与 SalePreOrderItemMapper 的既有写法一致）。</p>
     *
     * <p>查询条件与 {@code MallAdminServiceImpl#pageOrders} 同口径，全部可选（null/空串即不过滤）；
     * 明细行透出 {@code departmentName}（来源列 {@code erp_sale_order.dept_name}）。</p>
     *
     * @param page          分页对象
     * @param keyword       综合关键词（单据编号 / 客户 / 收货人 / 商品名称 / 商品货号）
     * @param orderStatus   订单状态集合（已由调用方把商城状态串/数字 status 归一为 erp_sale_order.status 整数）；
     *                      调用方传空集合时以 {@code -1} 占位表示「有筛选但无命中」
     * @param orderNo       单据编号（模糊）
     * @param startDate     单据日期(起)，含
     * @param endDate       单据日期(止)，调用方已按「含当日」补足为次日 00:00:00，故此处用 {@code <}
     * @param consignee     收货人（模糊）
     * @param paymentMethod 支付方式（等值）
     * @param orderSource   订单来源（等值 2/3）
     * @param productName   商品名称/货号（明细行模糊）
     */
    @Select("""
            <script>
            SELECT
                item.id                AS itemId,
                item.order_id          AS orderId,
                item.line_no           AS lineNo,
                item.product_id        AS productId,
                item.product_code      AS productCode,
                item.product_name      AS productName,
                item.barcode           AS barcode,
                item.small_unit_barcode AS smallUnitBarcode,
                item.specification     AS specification,
                item.model             AS modelNo,
                item.origin            AS originPlace,
                item.brand             AS brand,
                item.unit              AS unit,
                item.batch_code        AS batchBarcode,
                item.production_date   AS productionDate,
                item.expiry_date       AS expiryDate,
                item.quantity          AS saleQuantity,
                item.big_pack          AS bigPack,
                item.mid_pack          AS midPack,
                item.small_pack        AS smallPack,
                item.shipped_quantity  AS shippedItemQty,
                item.small_unit_quantity AS smallUnitQuantity,
                (COALESCE(item.quantity, 0) - COALESCE(item.shipped_quantity, 0)) AS unshippedItemQty,
                item.unit_price        AS unitPrice,
                item.small_unit_price  AS smallUnitPrice,
                item.amount            AS amount,
                item.remark            AS itemRemark,
                o.order_no             AS orderNo,
                o.order_date           AS orderDate,
                o.status               AS status,
                o.order_source         AS orderSource,
                o.customer_id          AS customerId,
                o.customer_name        AS customerName,
                o.total_amount         AS totalAmount,
                o.received_amount      AS receivedAmount,
                o.payment_method       AS paymentMethod,
                o.payment_status       AS paymentStatus,
                o.delivery_status      AS deliveryStatus,
                o.consignee            AS consignee,
                o.consignee_phone      AS consigneePhone,
                COALESCE(o.consignee_address, o.shipping_address) AS consigneeAddress,
                o.remark               AS sellerRemark,
                o.order_remark         AS orderRemark,
                o.buyer_remark         AS buyerRemark,
                o.pickup_address       AS pickupAddress,
                o.creator_name         AS creatorName,
                o.ext_info             AS extInfo,
                o.create_time          AS createTime,
                o.update_time          AS updateTime,
                -- 金标准「按明细」列所需单头字段（ErpSaleOrderMall 已映射的既有列）
                o.shipping_fee         AS freight,
                o.freight_payer        AS freightPayer,
                o.delivery_method      AS deliveryMethod,
                o.logistics_company    AS logisticsCompany,
                o.waybill_no           AS trackingNo,
                o.cod_amount           AS codAmount,
                o.expected_ship_time   AS expectedShipTime,
                o.total_quantity       AS productQuantity,
                o.warehouse_name       AS warehouseName,
                -- 部门名称（V9.49.0 建于 erp_sale_order.dept_name，Tab2「部门」列）
                o.dept_name            AS departmentName,
                o.salesman_name        AS handlerName,
                o.promoter_name        AS promoterName,
                o.print_count          AS printCount,
                o.bookkeeping_status   AS bookkeepingStatus,
                o.settled_amount       AS settledAmount,
                o.coupon_used          AS couponUsed,
                o.audit_time           AS auditTime,
                o.other_fee            AS otherFee,
                o.ext_num1             AS extNum1,
                o.ext_num2             AS extNum2,
                o.ext_text3            AS extText3,
                o.ext_text4            AS extText4,
                o.ext_text5            AS extText5
            FROM erp_sale_order_item item
            INNER JOIN erp_sale_order o ON item.order_id = o.id AND o.deleted = 0
            WHERE o.order_source IN (2, 3)
            <if test="keyword != null and keyword != ''">
                AND (
                    o.order_no LIKE CONCAT('%', #{keyword}, '%')
                    OR o.customer_name LIKE CONCAT('%', #{keyword}, '%')
                    OR o.consignee LIKE CONCAT('%', #{keyword}, '%')
                    OR item.product_name LIKE CONCAT('%', #{keyword}, '%')
                    OR item.product_code LIKE CONCAT('%', #{keyword}, '%')
                )
            </if>
            <if test="orderStatus != null and orderStatus.size() > 0">
                AND o.status IN
                <foreach collection="orderStatus" item="st" open="(" separator="," close=")">
                    #{st}
                </foreach>
            </if>
            <if test="orderNo != null and orderNo != ''">
                AND o.order_no LIKE CONCAT('%', #{orderNo}, '%')
            </if>
            <if test="startDate != null">
                AND o.order_date &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND o.order_date &lt; #{endDate}
            </if>
            <if test="consignee != null and consignee != ''">
                AND o.consignee LIKE CONCAT('%', #{consignee}, '%')
            </if>
            <if test="paymentMethod != null and paymentMethod != ''">
                AND o.payment_method = #{paymentMethod}
            </if>
            <if test="orderSource != null">
                AND o.order_source = #{orderSource}
            </if>
            <if test="productName != null and productName != ''">
                AND (
                    item.product_name LIKE CONCAT('%', #{productName}, '%')
                    OR item.product_code LIKE CONCAT('%', #{productName}, '%')
                )
            </if>
            ORDER BY o.create_time DESC, o.id DESC, item.line_no ASC
            </script>
            """)
    IPage<MallOrderItemPageDTO> selectDetailPage(Page<MallOrderItemPageDTO> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("orderStatus") List<Integer> orderStatus,
                                                 @Param("orderNo") String orderNo,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("consignee") String consignee,
                                                 @Param("paymentMethod") String paymentMethod,
                                                 @Param("orderSource") Integer orderSource,
                                                 @Param("productName") String productName);
}
