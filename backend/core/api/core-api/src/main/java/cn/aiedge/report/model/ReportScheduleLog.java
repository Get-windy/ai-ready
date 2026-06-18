package cn.aiedge.report.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_schedule_log")
public class ReportScheduleLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;

    private String reportId;

    private String executeStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long executeTime;

    private String errorMessage;

    private LocalDateTime createTime;
}
