package cn.aiedge.crm.marketing.enums;

public enum CampaignType {
    EMAIL(1, "邮件营销"),
    SMS(2, "短信营销"),
    WECHAT(3, "微信营销"),
    PHONE(4, "电话营销"),
    EVENT(5, "活动营销"),
    ONLINE(6, "线上推广"),
    OFFLINE(7, "线下推广"),
    CONTENT(8, "内容营销"),
    SOCIAL(9, "社交媒体"),
    MIXED(10, "综合营销");

    private final Integer code;
    private final String desc;

    CampaignType(Integer code, String desc) {
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