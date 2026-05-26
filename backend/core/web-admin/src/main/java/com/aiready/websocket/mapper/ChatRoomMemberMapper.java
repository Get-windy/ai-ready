package com.aiready.websocket.mapper;

import com.aiready.websocket.entity.ChatRoomMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天房间成员Mapper
 */
@Mapper
public interface ChatRoomMemberMapper extends BaseMapper<ChatRoomMember> {
    
    /**
     * 查询房间的所有成员
     */
    @Select("SELECT * FROM sys_chat_room_member WHERE room_id = #{roomId} AND status = 1 AND deleted = 0")
    List<ChatRoomMember> selectByRoomId(@Param("roomId") String roomId);
    
    /**
     * 查询用户在房间的信息
     */
    @Select("SELECT * FROM sys_chat_room_member WHERE room_id = #{roomId} AND user_id = #{userId} AND deleted = 0")
    ChatRoomMember selectByRoomAndUser(@Param("roomId") String roomId, @Param("userId") Long userId);
    
    /**
     * 查询用户参与的所有房间成员记录
     */
    @Select("SELECT * FROM sys_chat_room_member WHERE user_id = #{userId} AND status = 1 AND deleted = 0")
    List<ChatRoomMember> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 更新最后活跃时间
     */
    @Update("UPDATE sys_chat_room_member SET last_active_time = #{activeTime} " +
            "WHERE room_id = #{roomId} AND user_id = #{userId}")
    int updateLastActiveTime(@Param("roomId") String roomId, @Param("userId") Long userId, @Param("activeTime") LocalDateTime activeTime);
    
    /**
     * 更新最后阅读消息ID
     */
    @Update("UPDATE sys_chat_room_member SET last_read_message_id = #{messageId} " +
            "WHERE room_id = #{roomId} AND user_id = #{userId}")
    int updateLastReadMessageId(@Param("roomId") String roomId, @Param("userId") Long userId, @Param("messageId") Long messageId);
    
    /**
     * 增加未读消息数
     */
    @Update("UPDATE sys_chat_room_member SET unread_count = unread_count + 1 " +
            "WHERE room_id = #{roomId} AND user_id != #{excludeUserId} AND status = 1")
    int incrementUnreadCount(@Param("roomId") String roomId, @Param("excludeUserId") Long excludeUserId);
    
    /**
     * 清零未读消息数
     */
    @Update("UPDATE sys_chat_room_member SET unread_count = 0 " +
            "WHERE room_id = #{roomId} AND user_id = #{userId}")
    int clearUnreadCount(@Param("roomId") String roomId, @Param("userId") Long userId);
    
    /**
     * 统计房间成员数
     */
    @Select("SELECT COUNT(*) FROM sys_chat_room_member WHERE room_id = #{roomId} AND status = 1 AND deleted = 0")
    Integer countMembers(@Param("roomId") String roomId);
}
