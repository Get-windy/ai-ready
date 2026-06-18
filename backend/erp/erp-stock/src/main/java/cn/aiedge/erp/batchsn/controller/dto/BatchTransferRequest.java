package cn.aiedge.erp.batchsn.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 批次转移请求
 * 
 * @author team-member
 * @date 2026-05-05
 */
@Data
@Schema(description = "批次转移请求参数")
public class BatchTransferRequest {
    
    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    @Schema(description = "批次ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long batchId;
    
    /**
     * 源仓库ID
     */
    @NotNull(message = "源仓库ID不能为空")
    @Schema(description = "源仓库ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fromWarehouseId;
    
    /**
     * 目标仓库ID
     */
    @NotNull(message = "目标仓库ID不能为空")
    @Schema(description = "目标仓库ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long toWarehouseId;
    
    /**
     * 源库位ID（可选）
     */
    private Long fromLocationId;
    
    /**
     * 目标库位ID（可选）
     */
    private Long toLocationId;
    
    /**
     * 转移数量
     */
    @NotNull(message = "转移数量不能为空")
    @Schema(description = "转移数量", example = "10.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantity;
    
    /**
     * 转移备注
     */
    private String remark;
    
    /**
     * 操作人ID
     */
    @NotNull(message = "操作人ID不能为空")
    private String operatorId;
    
    /**
     * 操作人姓名
     */
    @NotNull(message = "操作人姓名不能为空")
    private String operatorName;
    
    /**
     * 验证请求参数
     */
    public void validate() {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("转移数量必须大于0");
        }
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new IllegalArgumentException("源仓库和目标仓库不能相同");
        }
    }
}