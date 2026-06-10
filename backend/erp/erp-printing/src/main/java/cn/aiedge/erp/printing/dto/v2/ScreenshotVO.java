package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScreenshotVO {

    private Long screenshotId;
    private String taskCode;
    private Long tenantId;
    private Long templateId;
    private String pageCode;
    private String imageUrl;
    private Integer imageWidth;
    private Integer imageHeight;
    private Long fileSize;
    private String status;
    private String errorMessage;
    private Integer durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
