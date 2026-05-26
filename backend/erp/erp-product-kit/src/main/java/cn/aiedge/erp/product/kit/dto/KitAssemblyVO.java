package cn.aiedge.erp.product.kit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class KitAssemblyVO {

    private Long id;

    private String assemblyNo;

    private Long kitId;

    private String kitCode;

    private String kitName;

    private Long productId;

    private String productCode;

    private String productName;

    private LocalDate assemblyDate;

    private Integer status;

    private String statusDesc;

    private BigDecimal assemblyQuantity;

    private BigDecimal totalCost;

    private Long warehouseId;

    private String warehouseName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long executedBy;

    private LocalDateTime executedTime;

    private Long completedBy;

    private LocalDateTime completedTime;

    private String remark;

    private String internalNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Integer versionNo;

    private List<?> items;
}