package cn.aiedge.automation.enums;

public enum ActionType {
    WRITE("WRITE", "更新字段"),
    CREATE("CREATE", "创建记录"),
    DELETE("DELETE", "删除记录"),
    SEND_EMAIL("SEND_EMAIL", "发送邮件"),
    SEND_MESSAGE("SEND_MESSAGE", "发送消息"),
    SEND_NOTIFICATION("SEND_NOTIFICATION", "发送通知"),
    EXECUTE_METHOD("EXECUTE_METHOD", "执行方法"),
    CREATE_ACTIVITY("CREATE_ACTIVITY", "创建活动"),
    ADD_FOLLOWER("ADD_FOLLOWER", "添加关注者"),
    SET_STATE("SET_STATE", "设置状态"),
    WEBHOOK("WEBHOOK", "调用Webhook");

    private final String code;
    private final String name;

    ActionType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}