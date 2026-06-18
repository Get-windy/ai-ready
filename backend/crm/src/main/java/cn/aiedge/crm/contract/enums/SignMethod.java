package cn.aiedge.crm.contract.enums;

public enum SignMethod {
    ONLINE(1, "在线签署"),
    OFFLINE(2, "线下签署"),
    ELECTRONIC(3, "电子签章"),
    MIXED(4, "混合签署");

    private final Integer code;
    private final String desc;

    SignMethod(Integer code, String desc) {
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