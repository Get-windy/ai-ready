package cn.aiedge.automation.enums;

public enum TriggerType {
    ON_CREATE("ON_CREATE", "创建时触发"),
    ON_WRITE("ON_WRITE", "更新时触发"),
    ON_DELETE("ON_DELETE", "删除时触发"),
    ON_TIME("ON_TIME", "定时触发"),
    ON_CHANGE("ON_CHANGE", "字段变更触发"),
    ON_STATE_CHANGE("ON_STATE_CHANGE", "状态变更触发");

    private final String code;
    private final String name;

    TriggerType(String code, String name) {
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