package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    /** 收货地址ID（从地址库选择） */
    private Long addressId;
    /** 收货人（如果未传addressId，直接使用此字段） */
    private String consignee;
    /** 联系电话 */
    private String phone;
    /** 收货地址 */
    private String address;
    /** 支付方式 */
    private String paymentMethod;
    /** 备注 */
    private String remark;
    /** 订单商品明细 */
    private List<OrderItemRequest> items;
}
