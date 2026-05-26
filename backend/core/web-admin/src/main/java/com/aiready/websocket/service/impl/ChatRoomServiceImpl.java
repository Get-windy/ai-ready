package com.aiready.websocket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.aiready.websocket.dto.ChatRoomDTO;
import com.aiready.websocket.dto.CreateRoomRequest;
import com.aiready.websocket.entity.ChatRoom;
import com.aiready.websocket.entity.ChatRoomMember;
import com.aiready.websocket.mapper.ChatRoomMapper;
import com.aiready.websocket.mapper.ChatRoomMemberMapper;
import com.aiready.websocket.service.ChatRoomService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 聊天房间服务实现
 */
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl extends ServiceImpl<ChatRoomMapper, ChatRoom> implements ChatRoomService {
    
    private final ChatRoomMapper chatRoomMapper;
    private final ChatRoomMemberMapper memberMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatRoomDTO createRoom(Long creatorId, CreateRoomRequest request) {
        // 生成房间ID
        String roomId = "ROOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // 创建房间
        ChatRoom room = new ChatRoom();
        room.setRoomId(roomId);
        room.setRoomName(request.getRoomName());
        room.setDescription(request.getDescription());
        room.setRoomType(request.getRoomType());
        room.setRoomAvatar(request.getRoomAvatar());
        room.setCreatorId(creatorId);
        room.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 500);
        room.setCurrentMembers(1); // 创建者
        room.setStatus(1);
        
        chatRoomMapper.insert(room);
        
        // 添加创建者为群主
        ChatRoomMember creator = new ChatRoomMember();
        creator.setRoomId(roomId);
        creator.setUserId(creatorId);
        creator.setRole(3); // 群主
        creator.setJoinTime(LocalDateTime.now());
        creator.setLastActiveTime(LocalDateTime.now());
        creator.setUnreadCount(0);
        creator.setStatus(1);
        memberMapper.insert(creator);
        
        // 添加其他成员
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    ChatRoomMember member = new ChatRoomMember();
                    member.setRoomId(roomId);
                    member.setUserId(memberId);
                    member.setRole(1); // 普通成员
                    member.setJoinTime(LocalDateTime.now());
                    member.setLastActiveTime(LocalDateTime.now());
                    member.setUnreadCount(0);
                    member.setStatus(1);
                    memberMapper.insert(member);
                }
            }
            // 更新成员数
            room.setCurrentMembers(1 + request.getMemberIds().size());
            chatRoomMapper.updateById(room);
        }
        
        return convertToDTO(room);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatRoomDTO createPrivateRoom(Long userId, Long targetUserId) {
        // 检查是否已存在私聊房间
        ChatRoom existingRoom = chatRoomMapper.selectPrivateRoom(userId, targetUserId);
        if (existingRoom != null) {
            return convertToDTO(existingRoom);
        }
        
        // 生成房间ID
        String roomId = "PRIV_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // 创建私聊房间
        ChatRoom room = new ChatRoom();
        room.setRoomId(roomId);
        room.setRoomName("私聊");
        room.setRoomType(1); // 私聊
        room.setCreatorId(userId);
        room.setMaxMembers(2);
        room.setCurrentMembers(2);
        room.setStatus(1);
        
        chatRoomMapper.insert(room);
        
        // 添加双方为成员
        ChatRoomMember member1 = new ChatRoomMember();
        member1.setRoomId(roomId);
        member1.setUserId(userId);
        member1.setRole(1);
        member1.setJoinTime(LocalDateTime.now());
        member1.setLastActiveTime(LocalDateTime.now());
        member1.setUnreadCount(0);
        member1.setStatus(1);
        memberMapper.insert(member1);
        
        ChatRoomMember member2 = new ChatRoomMember();
        member2.setRoomId(roomId);
        member2.setUserId(targetUserId);
        member2.setRole(1);
        member2.setJoinTime(LocalDateTime.now());
        member2.setLastActiveTime(LocalDateTime.now());
        member2.setUnreadCount(0);
        member2.setStatus(1);
        memberMapper.insert(member2);
        
        return convertToDTO(room);
    }
    
    @Override
    public ChatRoomDTO getRoomInfo(String roomId) {
        ChatRoom room = chatRoomMapper.selectByRoomId(roomId);
        return room != null ? convertToDTO(room) : null;
    }
    
    @Override
    public List<ChatRoomDTO> getUserRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomMapper.selectRoomsByUserId(userId);
        return rooms.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinRoom(String roomId, Long userId) {
        // 检查是否已在房间
        ChatRoomMember existing = memberMapper.selectByRoomAndUser(roomId, userId);
        if (existing != null) {
            if (existing.getStatus() == 0) {
                // 重新加入
                existing.setStatus(1);
                existing.setJoinTime(LocalDateTime.now());
                memberMapper.updateById(existing);
                chatRoomMapper.incrementMemberCount(roomId);
            }
            return;
        }
        
        // 添加新成员
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setRole(1); // 普通成员
        member.setJoinTime(LocalDateTime.now());
        member.setLastActiveTime(LocalDateTime.now());
        member.setUnreadCount(0);
        member.setStatus(1);
        memberMapper.insert(member);
        
        chatRoomMapper.incrementMemberCount(roomId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveRoom(String roomId, Long userId) {
        ChatRoomMember member = memberMapper.selectByRoomAndUser(roomId, userId);
        if (member != null && member.getStatus() == 1) {
            member.setStatus(0);
            memberMapper.updateById(member);
            chatRoomMapper.decrementMemberCount(roomId);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveRoom(String roomId, Long operatorId) {
        ChatRoom room = chatRoomMapper.selectByRoomId(roomId);
        if (room == null || !room.getCreatorId().equals(operatorId)) {
            throw new RuntimeException("无权解散该房间");
        }
        
        // 删除房间（逻辑删除）
        chatRoomMapper.deleteById(room.getId());
        
        // 删除所有成员
        List<ChatRoomMember> members = memberMapper.selectByRoomId(roomId);
        for (ChatRoomMember member : members) {
            memberMapper.deleteById(member.getId());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatRoomDTO updateRoom(String roomId, CreateRoomRequest request, Long operatorId) {
        ChatRoom room = chatRoomMapper.selectByRoomId(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        
        // 检查权限（群主或管理员）
        ChatRoomMember member = memberMapper.selectByRoomAndUser(roomId, operatorId);
        if (member == null || (member.getRole() != 2 && member.getRole() != 3)) {
            throw new RuntimeException("无权修改房间信息");
        }
        
        if (request.getRoomName() != null) {
            room.setRoomName(request.getRoomName());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }
        if (request.getRoomAvatar() != null) {
            room.setRoomAvatar(request.getRoomAvatar());
        }
        if (request.getMaxMembers() != null) {
            room.setMaxMembers(request.getMaxMembers());
        }
        
        chatRoomMapper.updateById(room);
        return convertToDTO(room);
    }
    
    @Override
    public ChatRoomDTO getPrivateRoom(Long userId, Long targetUserId) {
        ChatRoom room = chatRoomMapper.selectPrivateRoom(userId, targetUserId);
        return room != null ? convertToDTO(room) : null;
    }
    
    @Override
    public boolean isUserInRoom(String roomId, Long userId) {
        ChatRoomMember member = memberMapper.selectByRoomAndUser(roomId, userId);
        return member != null && member.getStatus() == 1;
    }
    
    @Override
    public List<Long> getRoomMemberIds(String roomId) {
        List<ChatRoomMember> members = memberMapper.selectByRoomId(roomId);
        return members.stream().map(ChatRoomMember::getUserId).collect(Collectors.toList());
    }
    
    private ChatRoomDTO convertToDTO(ChatRoom room) {
        ChatRoomDTO dto = new ChatRoomDTO();
        BeanUtil.copyProperties(room, dto);
        return dto;
    }
}
