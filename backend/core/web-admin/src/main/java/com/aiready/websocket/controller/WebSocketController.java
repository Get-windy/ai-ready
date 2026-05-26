package com.aiready.websocket.controller;

import com.aiready.websocket.dto.*;
import com.aiready.websocket.handler.WebSocketHandler;
import com.aiready.websocket.service.ChatRoomService;
import com.aiready.websocket.service.WebSocketMessageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket REST API控制器
 * 提供WebSocket相关的HTTP接口
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
@Tag(name = "WebSocket管理", description = "WebSocket实时通信管理接口")
public class WebSocketController {
    
    private final WebSocketMessageService messageService;
    private final ChatRoomService chatRoomService;
    private final WebSocketHandler webSocketHandler;
    
    /**
     * 发送系统通知
     */
    @PostMapping("/notify/{userId}")
    @Operation(summary = "发送系统通知")
    public WebSocketMessageDTO sendNotification(@PathVariable Long userId,
                                                 @RequestParam String title,
                                                 @RequestParam String content,
                                                 @RequestParam(required = false) String bizType,
                                                 @RequestParam(required = false) String bizId) {
        return messageService.sendSystemNotification(userId, title, content, bizType, bizId);
    }
    
    /**
     * 广播消息
     */
    @PostMapping("/broadcast")
    @Operation(summary = "广播消息")
    public void broadcastMessage(@RequestParam Long senderId,
                                  @RequestParam String content,
                                  @RequestParam(required = false) String bizType) {
        WebSocketMessageDTO message = messageService.broadcastMessage(senderId, content, bizType);
        webSocketHandler.broadcastToAll(WebSocketResponse.success("BROADCAST", message));
    }
    
    /**
     * 创建房间
     */
    @PostMapping("/room")
    @Operation(summary = "创建聊天房间")
    public ChatRoomDTO createRoom(@RequestParam Long creatorId,
                                   @RequestBody CreateRoomRequest request) {
        return chatRoomService.createRoom(creatorId, request);
    }
    
    /**
     * 创建私聊房间
     */
    @PostMapping("/room/private")
    @Operation(summary = "创建私聊房间")
    public ChatRoomDTO createPrivateRoom(@RequestParam Long userId,
                                          @RequestParam Long targetUserId) {
        return chatRoomService.createPrivateRoom(userId, targetUserId);
    }
    
    /**
     * 获取房间信息
     */
    @GetMapping("/room/{roomId}")
    @Operation(summary = "获取房间信息")
    public ChatRoomDTO getRoomInfo(@PathVariable String roomId) {
        return chatRoomService.getRoomInfo(roomId);
    }
    
    /**
     * 获取用户房间列表
     */
    @GetMapping("/rooms/{userId}")
    @Operation(summary = "获取用户房间列表")
    public List<ChatRoomDTO> getUserRooms(@PathVariable Long userId) {
        return chatRoomService.getUserRooms(userId);
    }
    
    /**
     * 加入房间
     */
    @PostMapping("/room/{roomId}/join")
    @Operation(summary = "加入房间")
    public void joinRoom(@PathVariable String roomId, @RequestParam Long userId) {
        chatRoomService.joinRoom(roomId, userId);
    }
    
    /**
     * 离开房间
     */
    @PostMapping("/room/{roomId}/leave")
    @Operation(summary = "离开房间")
    public void leaveRoom(@PathVariable String roomId, @RequestParam Long userId) {
        chatRoomService.leaveRoom(roomId, userId);
    }
    
    /**
     * 解散房间
     */
    @DeleteMapping("/room/{roomId}")
    @Operation(summary = "解散房间")
    public void dissolveRoom(@PathVariable String roomId, @RequestParam Long operatorId) {
        chatRoomService.dissolveRoom(roomId, operatorId);
    }
    
    /**
     * 更新房间信息
     */
    @PutMapping("/room/{roomId}")
    @Operation(summary = "更新房间信息")
    public ChatRoomDTO updateRoom(@PathVariable String roomId,
                                   @RequestParam Long operatorId,
                                   @RequestBody CreateRoomRequest request) {
        return chatRoomService.updateRoom(roomId, request, operatorId);
    }
    
    /**
     * 获取房间消息列表
     */
    @GetMapping("/room/{roomId}/messages")
    @Operation(summary = "获取房间消息列表")
    public IPage<WebSocketMessageDTO> getRoomMessages(@PathVariable String roomId,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "20") Integer size) {
        return messageService.getRoomMessages(roomId, page, size);
    }
    
    /**
     * 获取私聊消息
     */
    @GetMapping("/messages/private")
    @Operation(summary = "获取私聊消息")
    public IPage<WebSocketMessageDTO> getPrivateMessages(@RequestParam Long userId,
                                                          @RequestParam Long targetUserId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer size) {
        return messageService.getPrivateMessages(userId, targetUserId, page, size);
    }
    
    /**
     * 获取用户未读消息
     */
    @GetMapping("/messages/unread/{userId}")
    @Operation(summary = "获取用户未读消息")
    public List<WebSocketMessageDTO> getUnreadMessages(@PathVariable Long userId) {
        return messageService.getUnreadMessages(userId);
    }
    
    /**
     * 获取用户未读消息数
     */
    @GetMapping("/messages/unread-count/{userId}")
    @Operation(summary = "获取用户未读消息数")
    public Integer getUnreadCount(@PathVariable Long userId) {
        return messageService.getUnreadCount(userId);
    }
    
    /**
     * 标记消息为已读
     */
    @PostMapping("/message/{messageId}/read")
    @Operation(summary = "标记消息为已读")
    public void markAsRead(@PathVariable Long messageId, @RequestParam Long userId) {
        messageService.markAsRead(messageId, userId);
    }
    
    /**
     * 批量标记消息为已读
     */
    @PostMapping("/messages/read-batch")
    @Operation(summary = "批量标记消息为已读")
    public void markAsReadBatch(@RequestParam Long userId, @RequestBody List<Long> messageIds) {
        messageService.markAsReadBatch(messageIds, userId);
    }
    
    /**
     * 标记房间所有消息为已读
     */
    @PostMapping("/room/{roomId}/read-all")
    @Operation(summary = "标记房间所有消息为已读")
    public void markRoomAsRead(@PathVariable String roomId, @RequestParam Long userId) {
        messageService.markRoomAsRead(roomId, userId);
    }
    
    /**
     * 查询消息
     */
    @PostMapping("/messages/query")
    @Operation(summary = "查询消息")
    public IPage<WebSocketMessageDTO> queryMessages(@RequestBody MessageQueryRequest request) {
        return messageService.queryMessages(request);
    }
    
    /**
     * 获取在线统计
     */
    @GetMapping("/stats/online")
    @Operation(summary = "获取在线统计")
    public Map<String, Object> getOnlineStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("onlineUserCount", webSocketHandler.getOnlineUserCount());
        return stats;
    }
    
    /**
     * 获取房间在线人数
     */
    @GetMapping("/room/{roomId}/online-count")
    @Operation(summary = "获取房间在线人数")
    public Map<String, Object> getRoomOnlineCount(@PathVariable String roomId) {
        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomId);
        result.put("onlineCount", webSocketHandler.getRoomOnlineCount(roomId));
        return result;
    }
}
