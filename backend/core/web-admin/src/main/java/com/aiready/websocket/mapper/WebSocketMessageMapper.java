package com.aiready.websocket.mapper;

import com.aiready.websocket.entity.WebSocketMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket消息Mapper
 */
@Mapper
public interface WebSocketMessageMapper extends BaseMapper<WebSocketMessage> {
    
    /**
     * 查询房间消息列表
     */
    @Select("SELECT * FROM sys_websocket_message WHERE room_id = #{roomId} AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<WebSocketMessage> selectByRoomId(@Param("roomId") String roomId, 
                                           @Param("limit") Integer limit, 
                                           @Param("offset") Integer offset);
    
    /**
     * 查询用户私聊消息
     */
    @Select("SELECT * FROM sys_websocket_message WHERE ((sender_id = #{userId} AND receiver_id = #{targetUserId}) " +
            "OR (sender_id = #{targetUserId} AND receiver_id = #{userId})) AND message_type = 3 AND deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<WebSocketMessage> selectPrivateMessages(@Param("userId") Long userId, 
                                                  @Param("targetUserId") Long targetUserId,
                                                  @Param("limit") Integer limit, 
                                                  @Param("offset") Integer offset);
    
    /**
     * 查询用户未读消息
     */
    @Select("SELECT * FROM sys_websocket_message WHERE receiver_id = #{userId} AND read_status = 0 AND deleted = 0 " +
            "ORDER BY create_time DESC")
    List<WebSocketMessage> selectUnreadMessages(@Param("userId") Long userId);
    
    /**
     * 查询用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM sys_websocket_message WHERE receiver_id = #{userId} AND read_status = 0 AND deleted = 0")
    Integer countUnreadMessages(@Param("userId") Long userId);
    
    /**
     * 标记消息为已读
     */
    @Update("UPDATE sys_websocket_message SET read_status = 1, read_time = #{readTime} " +
            "WHERE id = #{messageId} AND receiver_id = #{userId}")
    int markAsRead(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
    
    /**
     * 批量标记消息为已读
     */
    @Update("UPDATE sys_websocket_message SET read_status = 1, read_time = #{readTime} " +
            "WHERE id IN (${messageIds}) AND receiver_id = #{userId}")
    int markAsReadBatch(@Param("messageIds") String messageIds, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
    
    /**
     * 标记房间所有消息为已读
     */
    @Update("UPDATE sys_websocket_message SET read_status = 1, read_time = #{readTime} " +
            "WHERE room_id = #{roomId} AND receiver_id = #{userId} AND read_status = 0")
    int markRoomMessagesAsRead(@Param("roomId") String roomId, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
    
    /**
     * 查询用户参与的所有房间的最新消息
     */
    @Select("SELECT m.* FROM sys_websocket_message m " +
            "INNER JOIN sys_chat_room_member rm ON m.room_id = rm.room_id " +
            "WHERE rm.user_id = #{userId} AND rm.status = 1 AND m.deleted = 0 " +
            "AND m.create_time > rm.join_time " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<WebSocketMessage> selectRecentMessages(@Param("userId") Long userId, @Param("limit") Integer limit);
}
