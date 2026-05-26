package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {

    /**
     * 查询用户的未读消息
     */
    List<SysMessage> selectUnreadByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 标记消息为已读
     */
    int markAsRead(@Param("messageId") Long messageId);

    /**
     * 批量标记消息为已读
     */
    int batchMarkAsRead(@Param("messageIds") List<Long> messageIds);

    /**
     * 查询待发送的消息
     */
    List<SysMessage> selectPendingMessages(@Param("limit") Integer limit);
}
