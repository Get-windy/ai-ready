package cn.aiedge.crm.contract.enums;

public enum ContractType {
    SALES(1, "销售合同"),
    PURCHASE(2, "采购合同"),
    SERVICE(3, "服务合同"),
    PROJECT(4, "项目合同"),
    FRAMEWORK(5, "框架合同"),
    PARTNER(6, "合作伙伴合同"),
    OTHER(7, "其他合同");

    private final Integer code;
    private final String desc;

    ContractType(Integer code, String desc) {
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