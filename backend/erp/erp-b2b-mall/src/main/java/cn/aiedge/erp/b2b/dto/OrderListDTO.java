package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderListDTO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String orderStatus;
    private LocalDateTime createdAt;
    private Integer itemCount;
}
