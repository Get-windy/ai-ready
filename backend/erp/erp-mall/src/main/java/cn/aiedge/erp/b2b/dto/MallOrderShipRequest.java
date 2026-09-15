package cn.aiedge.erp.b2b.dto;

import lombok.Data;

/**
 * 商城订单发货请求（POST /api/erp/mall/admin/order/{id}/ship）
 *
 * <p>与前端 {@code mallOrderApi.ship(id, { logisticsCompany, trackingNo, remark })} 一一对应。</p>
 *
 * <p>字段落点：物流公司 → {@code erp_sale_order.logistics_company}、
 * 运单号 → {@code erp_sale_order.waybill_no}（商城视图实体 ErpSaleOrderMall 已以
 * {@code logisticsCompany / trackingNo} 暴露），发货备注落单头 {@code remark}。</p>
 */
@Data
public class MallOrderShipRequest {

    /** 物流公司 */
    private String logisticsCompany;

    /** 运单号 */
    private String trackingNo;

    /** 发货备注 */
    private String remark;
}
