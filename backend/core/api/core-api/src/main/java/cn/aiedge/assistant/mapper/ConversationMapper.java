package cn.aiedge.assistant.mapper;

import cn.aiedge.assistant.entity.Conversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话记录数据访问层
 * 提供对话记录数据的 CRUD 操作
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    List<Conversation> selectBySessionId(@Param("sessionId") String sessionId, @Param("limit") Integer limit);

    int deleteBySessionId(@Param("sessionId") String sessionId);
}
