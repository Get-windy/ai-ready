package cn.aiedge.erp.batch.service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批次状态流转DTO
 * 用于状态变更操作
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchStateTransitionDTO {

    /**
     * 批次ID列表
     */
    @NotNull(message = "批次ID列表不能为空")
    private List<Long> batchIds;

    /**
     * 目标状态
     */
    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    /**
     * 变更原因
     */
    @NotBlank(message = "变更原因不能为空")
    private String reason;

    /**
     * 操作人ID
     */
    @NotBlank(message = "操作人ID不能为空")
    private String operatorId;

    /**
     * 操作人名称
     */
    @NotBlank(message = "操作人名称不能为空")
    private String operatorName;

    /**
     * 操作类型（手动、自动、质检等）
     */
    private String operationType = "MANUAL";

    /**
     * 质检报告ID（如果是质检操作）
     */
    private String inspectionReportId;

    /**
     * 质检报告URL
     */
    private String inspectionReportUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否强制变更（跳过状态流转验证）
     */
    private Boolean forceTransition = false;
}