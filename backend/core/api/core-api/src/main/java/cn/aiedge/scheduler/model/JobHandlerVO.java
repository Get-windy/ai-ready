package cn.aiedge.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定时任务处理器（可选执行目标，前端下拉用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobHandlerVO {

    /** 处理器键（存 scheduled_task.job_key） */
    private String key;

    /** 处理器名称（中文，展示用） */
    private String name;
}
