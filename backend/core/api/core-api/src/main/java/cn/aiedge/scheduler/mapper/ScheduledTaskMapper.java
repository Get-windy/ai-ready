package cn.aiedge.scheduler.mapper;

import cn.aiedge.scheduler.model.ScheduledTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 定时任务Mapper
 */
@Mapper
public interface ScheduledTaskMapper extends BaseMapper<ScheduledTask> {

    /**
     * 查询启用的任务列表
     */
    @Select("SELECT * FROM scheduled_task WHERE enabled = 1 AND deleted = 0")
    List<ScheduledTask> selectEnabledTasks();

    /**
     * 更新任务状态
     */
    @Update("UPDATE scheduled_task SET status = #{status}, update_time = NOW() WHERE id = #{taskId}")
    int updateStatus(@Param("taskId") Long taskId, @Param("status") String status);

    /**
     * 更新执行统计
     */
    @Update("UPDATE scheduled_task SET execute_count = execute_count + 1, " +
            "success_count = success_count + #{success}, " +
            "fail_count = fail_count + #{fail}, " +
            "last_execute_time = NOW(), " +
            "update_time = NOW() " +
            "WHERE id = #{taskId}")
    int updateExecuteStats(@Param("taskId") Long taskId, @Param("success") int success, @Param("fail") int fail);
}
