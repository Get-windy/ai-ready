package cn.aiedge.dms.task.mapper;

import cn.aiedge.dms.task.entity.DmsTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配送任务 Mapper
 */
@Mapper
public interface DmsTaskMapper extends BaseMapper<DmsTask> {

    /**
     * 在途任务涉及的全部租户（供**定时作业**逐租户执行）
     *
     * <p>后台线程没有登录上下文，多租户插件不会注入 tenant_id：若不逐租户切换上下文，
     * 阈值配置会读全局默认、审计落库会丢租户归属。口径与 {@code DispatchService.ACTIVE_STATUS}
     * 一致（已分配 1 / 已接单 2 / 取货中 3 / 配送中 4）。</p>
     */
    @Select("SELECT DISTINCT tenant_id FROM dms_task "
            + "WHERE deleted = 0 AND status IN (1, 2, 3, 4) AND tenant_id IS NOT NULL")
    List<Long> selectActiveTenantIds();
}
