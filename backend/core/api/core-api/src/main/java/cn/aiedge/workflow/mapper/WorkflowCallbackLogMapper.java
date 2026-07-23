package cn.aiedge.workflow.mapper;

import cn.aiedge.workflow.entity.WorkflowCallbackLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批回调补偿日志 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface WorkflowCallbackLogMapper extends BaseMapper<WorkflowCallbackLogEntity> {
}
