package cn.aiedge.crm.marketing.enums;

public enum ChannelType {
    EMAIL(1, "邮件"),
    SMS(2, "短信"),
    WECHAT(3, "微信"),
    PHONE(4, "电话"),
    WEBSITE(5, "网站"),
    APP(6, "APP"),
    SOCIAL_MEDIA(7, "社交媒体"),
    OFFLINE(8, "线下渠道");

    private final Integer code;
    private final String desc;

    ChannelType(Integer code, String desc) {
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