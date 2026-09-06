package cn.aiedge.wms.controller.dto;

import cn.aiedge.wms.entity.WmsReceiptDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 收货明细 VO：明细字段 + 单据级冗余字段（用于「按明细」列表展示/查询）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsReceiptDetailVO extends WmsReceiptDetail {
    @Schema(description = "单据号")
    private String taskNo;
    @Schema(description = "来源类型 0-采购入库 1-生产入库 2-退货入库 3-调拨入库 4-其他")
    private Integer sourceType;
    @Schema(description = "来源单号")
    private String sourceOrderNo;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "单据状态")
    private Integer docStatus;
    @Schema(description = "单据创建时间")
    private LocalDateTime taskCreateTime;
}
