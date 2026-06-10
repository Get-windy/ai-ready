package cn.aiedge.erp.printing.entity.v2;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_screenshot_task")
public class SysScreenshotTask {

    @TableId(type = IdType.AUTO)
    private Long screenshotId;

    private String taskCode;

    private Long tenantId;

    private Long templateId;

    private String dataJson;

    private String pageCode;

    private String imageUrl;

    private String imageBase64;

    private Integer imageWidth;

    private Integer imageHeight;

    private Long fileSize;

    private String status;

    private String errorMessage;

    private Integer durationMs;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
