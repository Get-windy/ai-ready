package cn.aiedge.erp.purchase.enums;

public enum InvitationStatus {
    
    DRAFT("DRAFT", "草稿"),
    PENDING_SEND("PENDING_SEND", "待发送"),
    SENT("SENT", "已发送"),
    RECEIVED("RECEIVED", "已接收"),
    VIEWED("VIEWED", "已查看"),
    ACCEPTED("ACCEPTED", "已接受"),
    DECLINED("DECLINED", "已拒绝"),
    QUOTING("QUOTING", "报价中"),
    QUOTED("QUOTED", "已报价"),
    EXPIRED("EXPIRED", "已过期"),
    WITHDRAWN("WITHDRAWN", "已撤回"),
    CANCELLED("CANCELLED", "已取消"),
    COMPLETED("COMPLETED", "已完成"),
    NEED_FOLLOW_UP("NEED_FOLLOW_UP", "需要跟进"),
    REMINDED("REMINDED", "已提醒");
    
    private final String code;
    private final String description;
    
    InvitationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static InvitationStatus fromCode(String code) {
        for (InvitationStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }
    
    public boolean isInProgress() {
        return this == SENT || this == RECEIVED || this == VIEWED || 
               this == ACCEPTED || this == QUOTING || this == NEED_FOLLOW_UP || this == REMINDED;
    }
    
    public boolean isCompleted() {
        return this == QUOTED || this == COMPLETED || this == DECLINED || 
               this == EXPIRED || this == WITHDRAWN || this == CANCELLED;
    }
    
    public boolean isValid() {
        return this != EXPIRED && this != WITHDRAWN && this != CANCELLED;
    }
    
    public boolean needsReminder() {
        return this == SENT || this == RECEIVED || this == VIEWED || this == ACCEPTED || this == QUOTING;
    }
    
    public boolean canSend() {
        return this == DRAFT || this == PENDING_SEND;
    }
    
    public boolean canWithdraw() {
        return this == SENT || this == RECEIVED || this == VIEWED || this == ACCEPTED || this == QUOTING;
    }
    
    public boolean canCancel() {
        return this != COMPLETED && this != CANCELLED && this != EXPIRED;
    }
    
    public static boolean canTransition(InvitationStatus from, InvitationStatus to) {
        return switch (from) {
            case DRAFT -> to == PENDING_SEND || to == CANCELLED;
            case PENDING_SEND -> to == SENT || to == CANCELLED;
            case SENT -> to == RECEIVED || to == VIEWED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED || to == NEED_FOLLOW_UP;
            case RECEIVED -> to == VIEWED || to == ACCEPTED || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case VIEWED -> to == ACCEPTED || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case ACCEPTED -> to == QUOTING || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case QUOTING -> to == QUOTED || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case QUOTED -> to == COMPLETED || to == CANCELLED;
            case DECLINED, EXPIRED, WITHDRAWN, CANCELLED, COMPLETED -> false;
            case NEED_FOLLOW_UP -> to == REMINDED || to == EXPIRED || to == CANCELLED;
            case REMINDED -> to == RECEIVED || to == VIEWED || to == ACCEPTED || to == DECLINED || to == EXPIRED || to == CANCELLED;
        };
    }
    
    public String getStatusDescription() {
        return switch (this) {
            case DRAFT -> "询价邀请已创建，等待发送";
            case PENDING_SEND -> "询价邀请已准备就绪，等待发送";
            case SENT -> "询价邀请已发送给供应商";
            case RECEIVED -> "供应商已接收询价邀请";
            case VIEWED -> "供应商已查看询价邀请";
            case ACCEPTED -> "供应商已接受询价邀请";
            case DECLINED -> "供应商拒绝参与报价";
            case QUOTING -> "供应商正在准备报价";
            case QUOTED -> "供应商已提交报价";
            case EXPIRED -> "询价邀请已过期";
            case WITHDRAWN -> "询价邀请已撤回";
            case CANCELLED -> "询价邀请已取消";
            case COMPLETED -> "询价邀请流程已完成";
            case NEED_FOLLOW_UP -> "需要跟进供应商响应";
            case REMINDED -> "已向供应商发送提醒";
        };
    }
    
    public String getStatusColor() {
        return switch (this) {
            case DRAFT -> "gray";
            case PENDING_SEND -> "orange";
            case SENT -> "blue";
            case RECEIVED -> "cyan";
            case VIEWED -> "blue";
            case ACCEPTED -> "green";
            case DECLINED -> "red";
            case QUOTING -> "purple";
            case QUOTED -> "green";
            case EXPIRED -> "gray";
            case WITHDRAWN -> "yellow";
            case CANCELLED -> "red";
            case COMPLETED -> "green";
            case NEED_FOLLOW_UP -> "orange";
            case REMINDED -> "blue";
        };
    }
    
    public String getSuggestedAction() {
        return switch (this) {
            case DRAFT -> "发送邀请";
            case PENDING_SEND -> "立即发送";
            case SENT -> "等待响应或发送提醒";
            case RECEIVED -> "等待供应商查看";
            case VIEWED -> "等待供应商接受";
            case ACCEPTED -> "等待报价提交";
            case DECLINED -> "记录拒绝原因";
            case QUOTING -> "等待报价或发送提醒";
            case QUOTED -> "评审报价";
            case EXPIRED -> "关闭邀请或重新发送";
            case WITHDRAWN -> "无需操作";
            case CANCELLED -> "无需操作";
            case COMPLETED -> "查看结果";
            case NEED_FOLLOW_UP -> "发送跟进提醒";
            case REMINDED -> "等待响应";
        };
    }
}
