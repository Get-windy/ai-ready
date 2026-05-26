package cn.aiedge.erp.purchase.enums;

import lombok.Getter;

/**
 * 询价邀请状态枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum InvitationStatus {
    
    /**
     * 草稿
     * - 邀请已创建但未发送
     * - 可以编辑和修改
     */
    DRAFT("DRAFT", "草稿"),
    
    /**
     * 待发送
     * - 邀请已准备就绪
     * - 等待发送给供应商
     */
    PENDING_SEND("PENDING_SEND", "待发送"),
    
    /**
     * 已发送
     * - 邀请已发送给供应商
     * - 等待供应商响应
     */
    SENT("SENT", "已发送"),
    
    /**
     * 已接收
     * - 供应商已接收邀请
     * - 正在准备报价
     */
    RECEIVED("RECEIVED", "已接收"),
    
    /**
     * 已查看
     * - 供应商已查看邀请
     * - 正在考虑是否报价
     */
    VIEWED("VIEWED", "已查看"),
    
    /**
     * 已接受
     * - 供应商已接受邀请
     * - 正在准备报价
     */
    ACCEPTED("ACCEPTED", "已接受"),
    
    /**
     * 已拒绝
     * - 供应商拒绝参与报价
     * - 需要记录拒绝原因
     */
    DECLINED("DECLINED", "已拒绝"),
    
    /**
     * 报价中
     * - 供应商正在准备报价
     * - 报价截止时间前
     */
    QUOTING("QUOTING", "报价中"),
    
    /**
     * 已报价
     * - 供应商已提交报价
     * - 等待评审
     */
    QUOTED("QUOTED", "已报价"),
    
    /**
     * 已过期
     * - 报价截止时间已过
     * - 供应商未及时响应
     */
    EXPIRED("EXPIRED", "已过期"),
    
    /**
     * 已撤回
     * - 采购方撤回邀请
     * - 供应商无法再响应
     */
    WITHDRAWN("WITHDRAWN", "已撤回"),
    
    /**
     * 已取消
     * - 采购需求取消
     * - 邀请被取消
     */
    CANCELLED("CANCELLED", "已取消"),
    
    /**
     * 已完成
     * - 邀请流程结束
     * - 已生成采购订单
     */
    COMPLETED("COMPLETED", "已完成"),
    
    /**
     * 需要跟进
     * - 供应商未及时响应
     * - 需要人工跟进
     */
    NEED_FOLLOW_UP("NEED_FOLLOW_UP", "需要跟进"),
    
    /**
     * 已提醒
     * - 已发送提醒给供应商
     * - 等待供应商响应
     */
    REMINDED("REMINDED", "已提醒");
    
    private final String code;
    private final String description;
    
    InvitationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static InvitationStatus fromCode(String code) {
        for (InvitationStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }
    
    /**
     * 判断是否为进行中状态
     */
    public boolean isInProgress() {
        return this == SENT || 
               this == RECEIVED || 
               this == VIEWED || 
               this == ACCEPTED || 
               this == QUOTING ||
               this == NEED_FOLLOW_UP ||
               this == REMINDED;
    }
    
    /**
     * 判断是否为已完成状态
     */
    public boolean isCompleted() {
        return this == QUOTED || 
               this == COMPLETED || 
               this == DECLINED || 
               this == EXPIRED || 
               this == WITHDRAWN || 
               this == CANCELLED;
    }
    
    /**
     * 判断是否为有效状态（可继续处理）
     */
    public boolean isValid() {
        return this != EXPIRED && 
               this != WITHDRAWN && 
               this != CANCELLED;
    }
    
    /**
     * 判断是否需要提醒
     */
    public boolean needsReminder() {
        return this == SENT || 
               this == RECEIVED || 
               this == VIEWED || 
               this == ACCEPTED || 
               this == QUOTING;
    }
    
    /**
     * 判断是否可以发送
     */
    public boolean canSend() {
        return this == DRAFT || this == PENDING_SEND;
    }
    
    /**
     * 判断是否可以撤回
     */
    public boolean canWithdraw() {
        return this == SENT || 
               this == RECEIVED || 
               this == VIEWED || 
               this == ACCEPTED || 
               this == QUOTING;
    }
    
    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this != COMPLETED && 
               this != CANCELLED && 
               this != EXPIRED;
    }
    
    /**
     * 获取状态转换规则
     */
    public static boolean canTransition(InvitationStatus from, InvitationStatus to) {
        // 状态转换规则
        return switch (from) {
            case DRAFT -> to == PENDING_SEND || to == CANCELLED;
            case PENDING_SEND -> to == SENT || to == CANCELLED;
            case SENT -> to == RECEIVED || to == VIEWED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED || to == NEED_FOLLOW_UP;
            case RECEIVED -> to == VIEWED || to == ACCEPTED || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case VIEWED -> to == ACCEPTED || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case ACCEPTED -> to == QUOTING || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case QUOTING -> to == QUOTED || to == DECLINED || to == EXPIRED || to == WITHDRAWN || to == CANCELLED;
            case QUOTED -> to == COMPLETED || to == CANCELLED;
            case DECLINED, EXPIRED, WITHDRAWN, CANCELLED, COMPLETED -> false; // 终态不可转换
            case NEED_FOLLOW_UP -> to == REMINDED || to == EXPIRED || to == CANCELLED;
            case REMINDED -> to == RECEIVED || to == VIEWED || to == ACCEPTED || to == DECLINED || to == EXPIRED || to == CANCELLED;
        };
    }
    
    /**
     * 获取状态描述
     */
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
    
    /**
     * 获取状态颜色（用于UI显示）
     */
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
    
    /**
     * 获取下一步建议操作
     */
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