package cn.aiedge.integration.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步记录实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "同步记录")
public class SyncRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @Schema(description = "记录ID")
    private String recordId;

    /**
     * 集成配置ID
     */
    @Schema(description = "集成配置ID")
    private String configId;

    /**
     * 同步类型: user/order/product
     */
    @Schema(description = "同步类型")
    private String syncType;

    /**
     * 操作类型: create/update/delete
     */
    @Schema(description = "操作类型")
    private String operation;

    /**
     * 数据ID
     */
    @Schema(description = "数据ID")
    private String dataId;

    /**
     * 外部系统数据ID
     */
    @Schema(description = "外部系统数据ID")
    private String externalId;

    /**
     * 状态: success/failed/pending
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 请求数据(JSON)
     */
    @Schema(description = "请求数据")
    private String requestData;

    /**
     * 响应数据(JSON)
     */
    @Schema(description = "响应数据")
    private String responseData;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getters and Setters
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }

    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getDataId() { return dataId; }
    public void setDataId(String dataId) { this.dataId = dataId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestData() { return requestData; }
    public void setRequestData(String requestData) { this.requestData = requestData; }

    public String getResponseData() { return responseData; }
    public void setResponseData(String responseData) { this.responseData = responseData; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
