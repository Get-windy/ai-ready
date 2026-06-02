package cn.aiedge.erp.b2b.dto;

import lombok.Data;

@Data
public class CartAddRequest {
    private String productId;
    private int quantity;
}
