package cn.aiedge.workflow.mapper;

import cn.aiedge.workflow.entity.WorkflowTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 工作流任务 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface WorkflowTaskMapper extends BaseMapper<WorkflowTaskEntity> {

    /**
     * 按节点分组的耗时/超时统计（仅审批任务）。
     * 超时判定：节点配置了时限 time_limit（小时）且实际处理时长超过该时限。
     *
     * @param processName 可选：按流程名称过滤（null / 空串 = 全部流程）。过滤链路为
     *                    workflow_task.instance_id → workflow_instance.definition_id → workflow_definition.process_name
     *                    （流程分析页「节点耗时分析」卡的流程下拉即走此参数，非假筛选）。
     *                    ⚠️ 工作流四表均在 MyBatisPlusConfig.IGNORE_TENANT_TABLES 内，多租户插件不会注入条件，
     *                    多出来的两个 JOIN 不会被追加 tenant_id 条件。
     */
    @Select("<script>"
            + "SELECT t.node_name AS node_name, COUNT(*) AS task_count, "
            + "COALESCE(AVG(EXTRACT(EPOCH FROM (t.handle_time - t.create_time))) "
            + "  FILTER (WHERE t.handle_time IS NOT NULL) / 3600.0, 0) AS avg_hours, "
            + "COUNT(*) FILTER (WHERE n.time_limit IS NOT NULL AND t.handle_time IS NOT NULL "
            + "  AND (t.handle_time - t.create_time) > n.time_limit * INTERVAL '1 hour') AS overdue_count "
            + "FROM workflow_task t LEFT JOIN workflow_node n ON t.node_id = n.id "
            + "LEFT JOIN workflow_instance i ON t.instance_id = i.id "
            + "LEFT JOIN workflow_definition d ON i.definition_id = d.id "
            + "WHERE t.deleted = 0 AND t.task_type = 1 "
            + "<if test=\"processName != null and processName != ''\">AND d.process_name = #{processName} </if>"
            + "GROUP BY t.node_name ORDER BY task_count DESC"
            + "</script>")
    List<Map<String, Object>> selectNodeDurations(@Param("processName") String processName);

    /**
     * 按日统计任务量/完成量/平均耗时/超时数（仅审批任务），用于效率报表。
     * 日期范围为闭区间 [startDate, endDate]，格式 YYYY-MM-DD。
     */
    @Select("SELECT TO_CHAR(t.create_time, 'YYYY-MM-DD') AS day, "
            + "COUNT(*) AS total_tasks, "
            + "COUNT(*) FILTER (WHERE t.status = 1) AS completed_tasks, "
            + "COALESCE(AVG(EXTRACT(EPOCH FROM (t.handle_time - t.create_time))) "
            + "  FILTER (WHERE t.handle_time IS NOT NULL) / 86400.0, 0) AS avg_days, "
            + "COUNT(*) FILTER (WHERE n.time_limit IS NOT NULL AND t.handle_time IS NOT NULL "
            + "  AND (t.handle_time - t.create_time) > n.time_limit * INTERVAL '1 hour') AS overdue_tasks "
            + "FROM workflow_task t LEFT JOIN workflow_node n ON t.node_id = n.id "
            + "WHERE t.deleted = 0 AND t.task_type = 1 "
            + "AND t.create_time >= #{startDate}::date AND t.create_time < (#{endDate}::date + 1) "
            + "GROUP BY 1 ORDER BY 1")
    List<Map<String, Object>> selectDailyTaskStats(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);
}
