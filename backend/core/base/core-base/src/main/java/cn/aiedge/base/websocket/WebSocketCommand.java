package cn.aiedge.base.websocket;

/**
 * WebSocket 命令协议
 * 与前端 src/types/websocket.ts 的 WsChatCommand 对齐
 */
public class WebSocketCommand {

    private String command;        // HEARTBEAT | JOIN_ROOM | LEAVE_ROOM | SEND_MESSAGE | READ_MESSAGE | GET_HISTORY
    private String roomId;
    private String messageId;
    private Long targetUserId;
    private String content;
    private Integer messageType;
    private Integer page;
    private Integer size;

    public WebSocketCommand() {}

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getMessageType() { return messageType; }
    public void setMessageType(Integer messageType) { this.messageType = messageType; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
