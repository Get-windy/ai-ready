package cn.aiedge.dms.event.mapper;

import cn.aiedge.dms.event.entity.DmsEventOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 事件发件箱 Mapper
 *
 * @author AI-Ready Team
 */
@Mapper
public interface DmsEventOutboxMapper extends BaseMapper<DmsEventOutbox> {

    /**
     * 查询待发送的待办事件
     *
     * 筛选状态为待发送且到达重试时间的事件，按创建时间排序，最多取100条。
     *
     * @return 待发送事件列表
     */
    @Select("SELECT * FROM dms_event_outbox " +
            "WHERE status = 0 " +
            "AND (next_retry_time IS NULL OR next_retry_time <= NOW()) " +
            "ORDER BY create_time ASC LIMIT 100")
    List<DmsEventOutbox> findPendingEvents();
}
