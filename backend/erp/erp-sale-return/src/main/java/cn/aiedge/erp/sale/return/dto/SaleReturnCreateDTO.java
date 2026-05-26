package cn.aiedge.erp.sale.return.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaleReturnCreateDTO {

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private LocalDate returnDate;

    private Integer returnType;

    private String returnReason;

    private Long warehouseId;

    private String warehouseName;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String remark;

    private String internalNote;

    private List<SaleReturnItemDTO> items;
}