package cn.aiedge.erp.b2b.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private String productId;
    private int quantity;
}
