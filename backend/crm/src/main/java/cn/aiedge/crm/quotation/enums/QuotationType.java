package cn.aiedge.crm.quotation.enums;

public enum QuotationType {
    STANDARD(1, "标准报价"),
    PROJECT(2, "项目报价"),
    TENDER(3, "招标报价"),
    REPEAT(4, "重复报价"),
    SPECIAL(5, "特殊报价");

    private final Integer code;
    private final String desc;

    QuotationType(Integer code, String desc) {
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