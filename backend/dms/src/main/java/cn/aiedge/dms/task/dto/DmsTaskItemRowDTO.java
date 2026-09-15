package cn.aiedge.dms.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 配送单「按明细」视图行（一个商品行 = 一行）
 */
@Data
@Schema(description = "配送单按明细视图行")
public class DmsTaskItemRowDTO {

    private Long id;

    private Long taskId;

    private String taskNo;

    private LocalDate deliveryDate;

    private Integer status;

    private String orderNo;

    private String customerName;

    private String riderName;

    private String vehicleName;

    private Integer lineNo;

    private String productCode;

    private String productName;

    private String barcode;

    private String spec;

    private String unit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private BigDecimal weight;

    private BigDecimal volume;

    private String remark;
}
