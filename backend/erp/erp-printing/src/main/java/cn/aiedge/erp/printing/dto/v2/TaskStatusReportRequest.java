package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatusReportRequest {

    @NotNull
    private Long taskId;

    @NotBlank
    private String status;

    private String errorMessage;

    private String resultLog;

    private Long printDuration;
}
