package cn.aiedge.erp.sale.salereturn.mapper;

import cn.aiedge.erp.sale.salereturn.dto.SaleReturnItemPageDTO;
import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaleReturnMapper extends BaseMapper<SaleReturn> {

    /**
     * 按明细分页（单头 LEFT JOIN 明细），返回强类型 {@link SaleReturnItemPageDTO}。
     *
     * <p>所有列均显式以双引号驼峰别名输出：本仓库 {@code map-underscore-to-camel-case}
     * 仅在 {@code application-local.yml} 中开启（dev/prod 为默认 false），因此不能依赖
     * 下划线→驼峰自动转换，必须由 SQL 固化字段名。</p>
     *
     * <p>列来源（均为已生效的既有列，未新增）：单头 {@code erp_sale_return}
     * r.* 包含 source_order / return_type / reason / approved_note / create_time 与表头自定义
     * ext_num1 / ext_num2 / ext_text3~5；明细 {@code erp_sale_return_item} i.* 包含
     * model_no / origin_place / brand / received_quantity / item_remark / ext_num1~5 / ext_text1~2。</p>
     */
    @Select({
            "<script>",
            "SELECT",
            "  i.id as \"itemId\",",
            "  r.id as \"id\",",
            "  i.line_no as \"lineNo\",",
            "  r.return_no as \"returnNo\",",
            "  r.order_date as \"orderDate\",",
            "  r.source_order as \"sourceOrder\",",
            "  r.status as \"status\",",
            "  r.return_type as \"returnType\",",
            "  r.settle_status as \"settleStatus\",",
            "  r.sales_type as \"salesType\",",
            "  r.generate_type as \"generateType\",",
            "  r.print_count as \"printCount\",",
            "  r.warehouse_name as \"warehouseName\",",
            "  r.customer_name as \"customerName\",",
            "  r.customer_code as \"customerCode\",",
            "  r.customer_level as \"customerLevel\",",
            "  r.contact_name as \"contactName\",",
            "  r.contact_phone as \"contactPhone\",",
            "  r.contact_address as \"contactAddress\",",
            "  r.customer_ticket as \"customerTicket\",",
            "  r.customer_remark as \"customerRemark\",",
            "  r.handler_name as \"handlerName\",",
            "  r.dept_name as \"deptName\",",
            "  r.remark as \"remark\",",
            // 审核备注：真实来源 = approved_note（审批意见/拒绝原因），库中无 audit_remark 列
            "  r.approved_note as \"approvedNote\",",
            "  r.summary as \"summary\",",
            "  r.attachment as \"attachment\",",
            "  r.creator_name as \"creatorName\",",
            "  r.auditor_name as \"auditorName\",",
            "  r.submit_by as \"submitBy\",",
            "  r.submit_time as \"submitTime\",",
            "  r.audit_time as \"auditTime\",",
            "  r.create_time as \"createTime\",",
            "  r.reason as \"reason\",",
            "  r.total_quantity as \"totalQuantity\",",
            "  r.total_amount as \"totalAmount\",",
            "  r.delivery_method as \"deliveryMethod\",",
            "  r.delivery_no as \"deliveryNo\",",
            "  r.waybill_no as \"waybillNo\",",
            "  r.logistics_company as \"logisticsCompany\",",
            "  r.shipping_fee as \"shippingFee\",",
            "  r.freight_payer as \"freightPayer\",",
            "  r.total_weight as \"totalWeight\",",
            "  r.total_volume as \"totalVolume\",",
            // 表头自定义字段（单头）——本页按明细列 headerExtNum1/2、headerExtText3~5
            "  r.ext_num1 as \"headerExtNum1\",",
            "  r.ext_num2 as \"headerExtNum2\",",
            "  r.ext_text3 as \"headerExtText3\",",
            "  r.ext_text4 as \"headerExtText4\",",
            "  r.ext_text5 as \"headerExtText5\",",
            // ── 明细行 ──
            "  i.product_name as \"productName\",",
            "  i.product_code as \"productCode\",",
            "  i.barcode as \"barcode\",",
            "  i.specification as \"specification\",",
            "  i.model_no as \"modelNo\",",
            "  i.origin_place as \"originPlace\",",
            "  i.brand as \"brand\",",
            "  i.image_url as \"imageUrl\",",
            "  i.unit as \"unit\",",
            "  i.small_unit as \"smallUnit\",",
            "  i.small_unit_quantity as \"smallUnitQuantity\",",
            "  i.conversion_relation as \"conversionRelation\",",
            "  i.conversion_result as \"conversionResult\",",
            "  i.big_pack as \"bigPack\",",
            "  i.mid_pack as \"midPack\",",
            "  i.small_pack as \"smallPack\",",
            "  i.piece_quantity as \"pieceQuantity\",",
            // 订货数量：明细表无独立列，取申请退货数量（主表 ordered_quantity 即该口径合计）
            "  i.return_quantity as \"orderedQuantity\",",
            "  i.return_quantity as \"returnQuantity\",",
            "  i.received_quantity as \"receivedQuantity\",",
            // 明细表未落库"未收数量"，按 退货数量-已收数量 派生（口径与主表 unreceived_quantity 一致）
            "  (i.return_quantity - COALESCE(i.received_quantity, 0)) as \"unreceivedQuantity\",",
            "  i.terminated_quantity as \"terminatedQuantity\",",
            "  i.terminated_amount as \"terminatedAmount\",",
            "  i.unit_price as \"unitPrice\",",
            "  i.line_amount as \"lineAmount\",",
            "  i.small_unit_price as \"smallUnitPrice\",",
            "  i.discount_rate as \"discountRate\",",
            "  i.discounted_price as \"discountedPrice\",",
            "  i.discounted_amount as \"discountedAmount\",",
            "  i.ref_cost_price as \"refCostPrice\",",
            "  i.ref_cost_amount as \"refCostAmount\",",
            "  i.weight as \"weight\",",
            "  i.volume as \"volume\",",
            "  i.is_gift as \"isGift\",",
            "  i.product_line_attr as \"productLineAttr\",",
            "  i.item_remark as \"itemRemark\",",
            "  i.exchange_gift as \"exchangeGift\",",
            "  i.exchange_points as \"exchangePoints\",",
            "  i.price_level1 as \"priceLevel1\",",
            "  i.price_level2 as \"priceLevel2\",",
            "  i.price_level3 as \"priceLevel3\",",
            "  i.price_level4 as \"priceLevel4\",",
            "  i.price_level5 as \"priceLevel5\",",
            "  i.price_level6 as \"priceLevel6\",",
            "  i.price_level7 as \"priceLevel7\",",
            "  i.price_level8 as \"priceLevel8\",",
            // 单据自定义字段（明细，命名兼容 sales/return-apply 按明细列口径）
            "  i.ext_num1 as \"extNum1\",",
            "  i.ext_num2 as \"extNum2\",",
            "  i.ext_num3 as \"extNum3\",",
            "  i.ext_num4 as \"extNum4\",",
            "  i.ext_num5 as \"extNum5\",",
            "  i.ext_text1 as \"extText1\",",
            "  i.ext_text2 as \"extText2\",",
            "  i.ext_partner as \"extPartner\",",
            "  i.ext_staff as \"extStaff\",",
            "  i.ext_dept as \"extDept\"",
            "FROM erp_sale_return r",
            "LEFT JOIN erp_sale_return_item i ON r.id = i.return_id AND i.deleted = 0",
            "${ew.customSqlSegment}",
            "ORDER BY r.order_date DESC, r.id DESC, i.line_no ASC",
            "</script>"
    })
    Page<SaleReturnItemPageDTO> selectPageDetail(
            Page<SaleReturnItemPageDTO> page,
            @Param(Constants.WRAPPER) QueryWrapper<SaleReturn> wrapper);

    /**
     * 取当天最后一张单据编号（号段自增用）。
     * <p>刻意绕过 {@code @TableLogic} 逻辑删除过滤：已软删除的单据仍占用编号唯一索引，
     * 若按 deleted=0 过滤会导致号段回退并撞唯一键。</p>
     */
    @Select("SELECT return_no FROM erp_sale_return WHERE return_no LIKE CONCAT(#{prefix}, '%') "
            + "ORDER BY return_no DESC LIMIT 1")
    String selectLastReturnNo(@Param("prefix") String prefix);
}
