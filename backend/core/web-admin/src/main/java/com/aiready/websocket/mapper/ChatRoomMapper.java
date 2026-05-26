package com.aiready.websocket.mapper;

import com.aiready.websocket.entity.ChatRoom;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 聊天房间Mapper
 */
@Mapper
public interface ChatRoomMapper extends BaseMapper<ChatRoom> {
    
    /**
     * 根据roomId查询房间
     */
    @Select("SELECT * FROM sys_chat_room WHERE room_id = #{roomId} AND deleted = 0")
    ChatRoom selectByRoomId(@Param("roomId") String roomId);
    
    /**
     * 查询用户参与的所有房间
     */
    @Select("SELECT r.* FROM sys_chat_room r " +
            "INNER JOIN sys_chat_room_member m ON r.room_id = m.room_id " +
            "WHERE m.user_id = #{userId} AND m.status = 1 AND r.deleted = 0 " +
            "ORDER BY r.last_message_time DESC")
    List<ChatRoom> selectRoomsByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户的私聊房间
     */
    @Select("SELECT r.* FROM sys_chat_room r " +
            "INNER JOIN sys_chat_room_member m1 ON r.room_id = m1.room_id AND m1.user_id = #{userId} " +
            "INNER JOIN sys_chat_room_member m2 ON r.room_id = m2.room_id AND m2.user_id = #{targetUserId} " +
            "WHERE r.room_type = 1 AND r.deleted = 0 AND m1.status = 1 AND m2.status = 1")
    ChatRoom selectPrivateRoom(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
    
    /**
     * 增加房间成员数
     */
    @Update("UPDATE sys_chat_room SET current_members = current_members + 1 WHERE room_id = #{roomId}")
    int incrementMemberCount(@Param("roomId") String roomId);
    
    /**
     * 减少房间成员数
     */
    @Update("UPDATE sys_chat_room SET current_members = current_members - 1 WHERE room_id = #{roomId}")
    int decrementMemberCount(@Param("roomId") String roomId);
    
    /**
     * 更新房间最后消息
     */
    @Update("UPDATE sys_chat_room SET last_message_time = NOW(), last_message_preview = #{preview} " +
            "WHERE room_id = #{roomId}")
    int updateLastMessage(@Param("roomId") String roomId, @Param("preview") String preview);
}
