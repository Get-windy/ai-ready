package cn.aiedge.erp.sale.salereturn.mapper;

import cn.aiedge.erp.sale.salereturn.entity.SaleReturn;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface SaleReturnMapper extends BaseMapper<SaleReturn> {

    @Select({
            "<script>",
            "SELECT",
            "  r.id, r.return_no, r.customer_name, r.customer_code, r.customer_level,",
            "  r.contact_name, r.contact_phone, r.contact_address,",
            "  r.customer_ticket, r.customer_remark,",
            "  r.warehouse_name, r.handler_name, r.dept_name,",
            "  r.order_date, r.status, r.settle_status, r.sales_type,",
            "  r.total_amount, r.total_quantity, r.generate_type, r.print_count,",
            "  r.submit_time, r.creator_name, r.auditor_name, r.audit_time,",
            "  r.summary, r.attachment, r.remark, r.delivery_method, r.delivery_no,",
            "  r.waybill_no, r.logistics_company, r.shipping_fee, r.freight_payer,",
            "  r.total_weight, r.total_volume, r.submit_by,",
            "  i.product_name, i.product_code, i.barcode, i.specification, i.model_no, i.origin_place,",
            "  i.unit, i.small_unit, i.small_unit_quantity, i.conversion_relation, i.conversion_result,",
            "  i.return_quantity, i.received_quantity,",
            // 明细表未落库"未收数量"，按 退货数量-已收数量 派生（口径与主表 unreceived_quantity 一致）
            "  (i.return_quantity - COALESCE(i.received_quantity, 0)) as \"unreceivedQuantity\",",
            "  i.terminated_quantity, i.terminated_amount,",
            "  i.big_pack, i.mid_pack, i.small_pack, i.piece_quantity,",
            "  i.unit_price, i.line_amount, i.discount_rate, i.discounted_price, i.discounted_amount,",
            "  i.small_unit_price, i.ref_cost_price, i.ref_cost_amount,",
            "  i.weight, i.volume,",
            "  i.image_url, i.brand, i.is_gift, i.product_line_attr,",
            "  i.item_remark, i.exchange_gift, i.exchange_points,",
            "  i.price_level1, i.price_level2, i.price_level3, i.price_level4,",
            "  i.price_level5, i.price_level6, i.price_level7, i.price_level8,",
            "  i.ext_num1 as \"extNum1\", i.ext_num2 as \"extNum2\", i.ext_num3 as \"extNum3\",",
            "  i.ext_num4 as \"extNum4\", i.ext_num5 as \"extNum5\",",
            "  i.ext_partner as \"extPartner\", i.ext_staff as \"extStaff\", i.ext_dept as \"extDept\",",
            "  i.ext_text1 as \"extText1\", i.ext_text2 as \"extText2\"",
            "FROM erp_sale_return r",
            "LEFT JOIN erp_sale_return_item i ON r.id = i.return_id AND i.deleted = 0",
            "${ew.customSqlSegment}",
            "ORDER BY r.order_date DESC, r.id DESC, i.line_no ASC",
            "</script>"
    })
    Page<Map<String, Object>> selectPageDetail(
            Page<Map<String, Object>> page,
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
