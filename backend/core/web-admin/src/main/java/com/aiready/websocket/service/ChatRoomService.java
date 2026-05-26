package com.aiready.websocket.service;

import com.aiready.websocket.dto.ChatRoomDTO;
import com.aiready.websocket.dto.CreateRoomRequest;
import com.aiready.websocket.entity.ChatRoom;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 聊天房间服务接口
 */
public interface ChatRoomService extends IService<ChatRoom> {
    
    /**
     * 创建房间
     */
    ChatRoomDTO createRoom(Long creatorId, CreateRoomRequest request);
    
    /**
     * 创建私聊房间
     */
    ChatRoomDTO createPrivateRoom(Long userId, Long targetUserId);
    
    /**
     * 获取房间信息
     */
    ChatRoomDTO getRoomInfo(String roomId);
    
    /**
     * 获取用户参与的所有房间
     */
    List<ChatRoomDTO> getUserRooms(Long userId);
    
    /**
     * 加入房间
     */
    void joinRoom(String roomId, Long userId);
    
    /**
     * 离开房间
     */
    void leaveRoom(String roomId, Long userId);
    
    /**
     * 解散房间
     */
    void dissolveRoom(String roomId, Long operatorId);
    
    /**
     * 更新房间信息
     */
    ChatRoomDTO updateRoom(String roomId, CreateRoomRequest request, Long operatorId);
    
    /**
     * 获取用户的私聊房间
     */
    ChatRoomDTO getPrivateRoom(Long userId, Long targetUserId);
    
    /**
     * 检查用户是否在房间中
     */
    boolean isUserInRoom(String roomId, Long userId);
    
    /**
     * 获取房间成员ID列表
     */
    List<Long> getRoomMemberIds(String roomId);
}
