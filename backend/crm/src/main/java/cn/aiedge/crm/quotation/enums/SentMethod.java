package cn.aiedge.crm.quotation.enums;

public enum SentMethod {
    EMAIL(1, "邮件"),
    WECHAT(2, "微信"),
    SMS(3, "短信"),
    FAX(4, "传真"),
    HAND_DELIVERY(5, "人工送达"),
    ONLINE(6, "在线查看");

    private final Integer code;
    private final String desc;

    SentMethod(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}