package com.aiready.websocket;

import com.aiready.websocket.dto.*;
import com.aiready.websocket.entity.ChatRoom;
import com.aiready.websocket.entity.ChatRoomMember;
import com.aiready.websocket.entity.WebSocketMessage;
import com.aiready.websocket.mapper.ChatRoomMapper;
import com.aiready.websocket.mapper.ChatRoomMemberMapper;
import com.aiready.websocket.mapper.WebSocketMessageMapper;
import com.aiready.websocket.service.ChatRoomService;
import com.aiready.websocket.service.WebSocketMessageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket模块单元测试
 */
@SpringBootTest
@Transactional
public class WebSocketModuleTest {
    
    @Autowired
    private WebSocketMessageService messageService;
    
    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private WebSocketMessageMapper messageMapper;
    
    @Autowired
    private ChatRoomMapper chatRoomMapper;
    
    @Autowired
    private ChatRoomMemberMapper memberMapper;
    
    @Test
    public void testSendMessage() {
        // 准备数据
        SendMessageRequest request = new SendMessageRequest();
        request.setMessageType(3); // 私聊消息
        request.setReceiverId(2L);
        request.setContent("测试消息内容");
        request.setContentType(1);
        request.setBizType("TEST");
        
        // 执行
        WebSocketMessageDTO result = messageService.sendMessage(1L, request);
        
        // 验证
        assertNotNull(result);
        assertNotNull(result.getMessageId());
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getReceiverId());
        assertEquals("测试消息内容", result.getContent());
        assertEquals(0, result.getReadStatus());
    }
    
    @Test
    public void testSendSystemNotification() {
        // 执行
        WebSocketMessageDTO result = messageService.sendSystemNotification(
                1L, "系统通知", "这是一条系统通知", "SYSTEM", "123");
        
        // 验证
        assertNotNull(result);
        assertEquals(0L, result.getSenderId()); // 系统发送
        assertEquals("系统", result.getSenderName());
        assertEquals(1, result.getMessageType()); // 系统通知
    }
    
    @Test
    public void testBroadcastMessage() {
        // 执行
        WebSocketMessageDTO result = messageService.broadcastMessage(1L, "广播消息内容", "BROADCAST");
        
        // 验证
        assertNotNull(result);
        assertEquals(5, result.getMessageType()); // 广播消息
        assertEquals("广播消息内容", result.getContent());
    }
    
    @Test
    public void testGetRoomMessages() {
        // 先创建房间和消息
        CreateRoomRequest roomRequest = new CreateRoomRequest();
        roomRequest.setRoomName("测试房间");
        roomRequest.setRoomType(2);
        ChatRoomDTO room = chatRoomService.createRoom(1L, roomRequest);
        
        // 发送几条消息
        for (int i = 0; i < 5; i++) {
            SendMessageRequest request = new SendMessageRequest();
            request.setMessageType(4); // 群聊消息
            request.setRoomId(room.getRoomId());
            request.setContent("消息" + i);
            messageService.sendMessage(1L, request);
        }
        
        // 执行查询
        IPage<WebSocketMessageDTO> result = messageService.getRoomMessages(room.getRoomId(), 1, 10);
        
        // 验证
        assertNotNull(result);
        assertTrue(result.getTotal() >= 5);
    }
    
    @Test
    public void testGetPrivateMessages() {
        // 发送私聊消息
        SendMessageRequest request1 = new SendMessageRequest();
        request1.setMessageType(3);
        request1.setReceiverId(2L);
        request1.setContent("用户1发给用户2");
        messageService.sendMessage(1L, request1);
        
        SendMessageRequest request2 = new SendMessageRequest();
        request2.setMessageType(3);
        request2.setReceiverId(1L);
        request2.setContent("用户2发给用户1");
        messageService.sendMessage(2L, request2);
        
        // 执行查询
        IPage<WebSocketMessageDTO> result = messageService.getPrivateMessages(1L, 2L, 1, 10);
        
        // 验证
        assertNotNull(result);
        assertTrue(result.getTotal() >= 2);
    }
    
    @Test
    public void testMarkAsRead() {
        // 发送消息
        SendMessageRequest request = new SendMessageRequest();
        request.setMessageType(3);
        request.setReceiverId(2L);
        request.setContent("未读消息");
        WebSocketMessageDTO message = messageService.sendMessage(1L, request);
        
        // 查询未读数
        Integer unreadCountBefore = messageService.getUnreadCount(2L);
        assertTrue(unreadCountBefore > 0);
        
        // 获取消息ID并标记已读
        WebSocketMessage entity = messageMapper.selectById(message.getMessageId());
        if (entity != null) {
            messageService.markAsRead(entity.getId(), 2L);
        }
    }
    
    @Test
    public void testCreateRoom() {
        // 准备数据
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomName("测试群聊");
        request.setDescription("这是一个测试群聊");
        request.setRoomType(2);
        request.setMaxMembers(100);
        request.setMemberIds(Arrays.asList(2L, 3L));
        
        // 执行
        ChatRoomDTO result = chatRoomService.createRoom(1L, request);
        
        // 验证
        assertNotNull(result);
        assertNotNull(result.getRoomId());
        assertEquals("测试群聊", result.getRoomName());
        assertEquals(2, result.getRoomType());
        assertEquals(3, result.getCurrentMembers()); // 创建者 + 2个成员
    }
    
    @Test
    public void testCreatePrivateRoom() {
        // 执行
        ChatRoomDTO result = chatRoomService.createPrivateRoom(1L, 2L);
        
        // 验证
        assertNotNull(result);
        assertNotNull(result.getRoomId());
        assertEquals(1, result.getRoomType()); // 私聊
        assertEquals(2, result.getCurrentMembers());
        assertEquals(2, result.getMaxMembers());
    }
    
    @Test
    public void testGetUserRooms() {
        // 创建房间
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomName("用户1的房间");
        request.setRoomType(2);
        chatRoomService.createRoom(1L, request);
        
        // 执行查询
        List<ChatRoomDTO> result = chatRoomService.getUserRooms(1L);
        
        // 验证
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    public void testJoinAndLeaveRoom() {
        // 创建房间
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomName("测试房间");
        request.setRoomType(2);
        ChatRoomDTO room = chatRoomService.createRoom(1L, request);
        
        // 用户2加入
        chatRoomService.joinRoom(room.getRoomId(), 2L);
        
        // 验证成员数
        ChatRoomDTO updatedRoom = chatRoomService.getRoomInfo(room.getRoomId());
        assertEquals(2, updatedRoom.getCurrentMembers());
        
        // 验证用户是否在房间
        assertTrue(chatRoomService.isUserInRoom(room.getRoomId(), 2L));
        
        // 用户2离开
        chatRoomService.leaveRoom(room.getRoomId(), 2L);
        
        // 验证
        assertFalse(chatRoomService.isUserInRoom(room.getRoomId(), 2L));
    }
    
    @Test
    public void testQueryMessages() {
        // 发送消息
        SendMessageRequest request = new SendMessageRequest();
        request.setMessageType(1);
        request.setReceiverId(2L);
        request.setContent("查询测试");
        request.setBizType("QUERY_TEST");
        messageService.sendMessage(1L, request);
        
        // 构建查询条件
        MessageQueryRequest queryRequest = new MessageQueryRequest();
        queryRequest.setSenderId(1L);
        queryRequest.setReceiverId(2L);
        queryRequest.setBizType("QUERY_TEST");
        queryRequest.setPage(1);
        queryRequest.setSize(10);
        
        // 执行查询
        IPage<WebSocketMessageDTO> result = messageService.queryMessages(queryRequest);
        
        // 验证
        assertNotNull(result);
        assertTrue(result.getTotal() > 0);
    }
    
    @Test
    public void testEntityMapping() {
        // 测试实体类创建
        WebSocketMessage message = new WebSocketMessage();
        message.setMessageId("TEST_MSG_ID");
        message.setContent("测试内容");
        message.setMessageType(1);
        message.setSenderId(1L);
        
        assertNotNull(message);
        assertEquals("TEST_MSG_ID", message.getMessageId());
        assertEquals("测试内容", message.getContent());
        
        // 测试房间实体
        ChatRoom room = new ChatRoom();
        room.setRoomId("TEST_ROOM_ID");
        room.setRoomName("测试房间");
        room.setRoomType(2);
        
        assertNotNull(room);
        assertEquals("TEST_ROOM_ID", room.getRoomId());
        
        // 测试成员实体
        ChatRoomMember member = new ChatRoomMember();
        member.setRoomId("TEST_ROOM_ID");
        member.setUserId(1L);
        member.setRole(1);
        
        assertNotNull(member);
        assertEquals("TEST_ROOM_ID", member.getRoomId());
    }
}
