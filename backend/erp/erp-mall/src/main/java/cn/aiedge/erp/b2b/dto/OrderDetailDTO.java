package cn.aiedge.erp.b2b.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailDTO extends OrderListDTO {
    private String customerName;
    private String consignee;
    private String phone;
    private String address;
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long id;
        private String productId;
        private String productName;
        private String productImage;
        private java.math.BigDecimal price;
        private Integer quantity;
        private java.math.BigDecimal subtotal;
    }
}
