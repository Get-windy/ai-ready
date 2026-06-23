package cn.aiedge.erp.sale.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_sale_outbound_item")
public class SaleOutboundItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long outboundId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long orderItemId;

    private BigDecimal orderQuantity;

    private BigDecimal outboundQuantity;

    private BigDecimal pendingQuantity;

    private BigDecimal unitPrice;

    private BigDecimal lineAmount;

    private String batchNo;

    private LocalDateTime productionDate;

    private LocalDateTime validityDate;

    private Integer warehouseLocationId;

    private String warehouseLocationCode;

    private Integer pickingStatus;

    private Long pickingBy;

    private LocalDateTime pickingTime;

    private Integer packingStatus;

    private Long packingBy;

    private LocalDateTime packingTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}