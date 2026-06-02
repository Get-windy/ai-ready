package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    private Long addressId;
    private String remark;
    private List<OrderItemRequest> items;
}
