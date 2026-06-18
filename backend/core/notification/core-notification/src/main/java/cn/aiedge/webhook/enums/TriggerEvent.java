package cn.aiedge.webhook.enums;

public enum TriggerEvent {
    ON_CREATE("ON_CREATE", "创建时触发"),
    ON_UPDATE("ON_UPDATE", "更新时触发"),
    ON_DELETE("ON_DELETE", "删除时触发"),
    ON_STATE_CHANGE("ON_STATE_CHANGE", "状态变更触发"),
    ON_FIELD_CHANGE("ON_FIELD_CHANGE", "字段变更触发"),
    ON_SCHEDULED("ON_SCHEDULED", "定时触发");

    private final String code;
    private final String name;

    TriggerEvent(String code, String name) {
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