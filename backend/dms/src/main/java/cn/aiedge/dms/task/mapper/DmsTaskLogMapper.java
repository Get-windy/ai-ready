package cn.aiedge.dms.task.mapper;

import cn.aiedge.dms.task.entity.DmsTaskLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调度审计日志 Mapper
 */
@Mapper
public interface DmsTaskLogMapper extends BaseMapper<DmsTaskLog> {
}
