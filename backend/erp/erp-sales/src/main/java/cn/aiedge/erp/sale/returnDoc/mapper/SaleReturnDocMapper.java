package cn.aiedge.erp.sale.returnDoc.mapper;

import cn.aiedge.erp.sale.returnDoc.entity.SaleReturnDoc;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface SaleReturnDocMapper extends BaseMapper<SaleReturnDoc> {

    @Select({
            "<script>",
            "SELECT",
            "  r.id, r.return_doc_no, r.customer_name, r.customer_code, r.customer_level,",
            "  r.contact_name, r.contact_phone, r.contact_address,",
            "  r.customer_ticket, r.customer_remark,",
            "  r.warehouse_name, r.handler_name, r.dept_name,",
            "  r.order_date, r.status, r.settle_status, r.sales_type,",
            "  r.total_amount, r.total_quantity, r.generate_type, r.print_count,",
            "  r.submit_time, r.creator_name, r.auditor_name, r.audit_time,",
            "  r.summary, r.attachment, r.remark, r.delivery_method, r.delivery_no,",
            "  r.waybill_no, r.logistics_company, r.shipping_fee, r.freight_payer,",
            "  r.total_weight, r.total_volume, r.submit_by,",
            "  r.discount_bill_amount, r.settled_amount, r.bill_amount,",
            "  r.receiver_name, r.receiver_phone, r.shipping_address,",
            "  r.source_order, r.return_apply_no,",
            "  r.ext_num1, r.ext_num2, r.ext_text1, r.ext_text2, r.ext_text3,",
            "  r.bookkeeping_time, r.print_time,",
            "  r.product_line_count,",
            "  i.product_name, i.product_code, i.barcode, i.specification, i.model_no, i.origin_place,",
            "  i.unit, i.small_unit, i.small_unit_quantity, i.conversion_relation, i.conversion_result,",
            "  i.return_quantity, i.received_quantity,",
            "  i.terminated_quantity, i.terminated_amount,",
            "  i.big_pack, i.mid_pack, i.small_pack, i.piece_quantity,",
            "  i.unit_price, i.line_amount, i.discount_rate, i.discounted_price, i.discounted_amount,",
            "  i.small_unit_price, i.ref_cost_price, i.ref_cost_amount,",
            "  i.weight, i.volume,",
            "  i.image_url, i.brand, i.is_gift, i.product_line_attr,",
            "  i.item_remark, i.exchange_gift, i.exchange_points,",
            "  i.price_level1, i.price_level2, i.price_level3, i.price_level4,",
            "  i.price_level5, i.price_level6, i.price_level7, i.price_level8,",
            "  i.ext_num1 as i_ext_num1, i.ext_num2 as i_ext_num2, i.ext_num3 as i_ext_num3,",
            "  i.ext_text1 as i_ext_text1, i.ext_text2 as i_ext_text2",
            "FROM erp_sale_return_doc r",
            "LEFT JOIN erp_sale_return_doc_item i ON r.id = i.return_doc_id AND i.deleted = 0",
            "${ew.customSqlSegment}",
            "ORDER BY r.order_date DESC, r.id DESC, i.line_no ASC",
            "</script>"
    })
    Page<Map<String, Object>> selectPageDetail(
            Page<Map<String, Object>> page,
            @Param(Constants.WRAPPER) QueryWrapper<SaleReturnDoc> wrapper);
}
