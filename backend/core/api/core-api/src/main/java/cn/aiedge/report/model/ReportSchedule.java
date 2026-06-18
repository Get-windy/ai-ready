package cn.aiedge.report.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_schedule")
public class ReportSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportId;

    private String reportName;

    private String scheduleCron;

    private String emailRecipients;

    private String exportFormat;

    private String parameters;

    private String status;

    private Integer enabled;

    private LocalDateTime lastExecuteTime;

    private LocalDateTime nextExecuteTime;

    private Integer executeCount;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
