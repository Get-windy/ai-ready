package cn.aiedge.erp.sale.outbound.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaleOutboundCreateDTO {

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private LocalDate outboundDate;

    private Integer outboundType;

    private Long warehouseId;

    private String warehouseName;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String shippingAddress;

    private String receiverName;

    private String receiverPhone;

    private String remark;

    private String internalNote;

    private List<SaleOutboundItemDTO> items;
}