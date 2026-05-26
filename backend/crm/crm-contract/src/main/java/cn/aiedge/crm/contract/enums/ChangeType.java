package cn.aiedge.crm.contract.enums;

public enum ChangeType {
    AMOUNT(1, "金额变更"),
    DATE(2, "日期变更"),
    CONTENT(3, "内容变更"),
    PARTY(4, "当事人变更"),
    TERMINATION(5, "终止变更"),
    EXTENSION(6, "延期变更"),
    OTHER(7, "其他变更");

    private final Integer code;
    private final String desc;

    ChangeType(Integer code, String desc) {
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