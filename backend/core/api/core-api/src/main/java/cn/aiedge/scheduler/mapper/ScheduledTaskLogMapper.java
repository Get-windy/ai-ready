package cn.aiedge.scheduler.mapper;

import cn.aiedge.scheduler.model.ScheduledTaskLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务日志Mapper
 */
@Mapper
public interface ScheduledTaskLogMapper extends BaseMapper<ScheduledTaskLog> {

    /**
     * 分页查询任务日志
     */
    @Select("<script>" +
            "SELECT * FROM scheduled_task_log WHERE 1=1 " +
            "<if test='taskId != null'> AND task_id = #{taskId} </if>" +
            "<if test='status != null'> AND execute_status = #{status} </if>" +
            "<if test='startTime != null'> AND start_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND start_time &lt;= #{endTime} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    IPage<ScheduledTaskLog> selectLogPage(Page<ScheduledTaskLog> page,
                                          @Param("taskId") Long taskId,
                                          @Param("status") String status,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 查询任务最近日志
     */
    @Select("SELECT * FROM scheduled_task_log WHERE task_id = #{taskId} ORDER BY create_time DESC LIMIT #{limit}")
    List<ScheduledTaskLog> selectRecentLogs(@Param("taskId") Long taskId, @Param("limit") int limit);

    /**
     * 统计任务执行次数
     */
    @Select("SELECT COUNT(*) FROM scheduled_task_log WHERE task_id = #{taskId} AND execute_status = #{status}")
    Long countByStatus(@Param("taskId") Long taskId, @Param("status") String status);
}
