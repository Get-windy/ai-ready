package cn.aiedge.erp.signature.enums;

public enum SignatureStatus {
    PENDING("PENDING", "待签收"),
    SIGNED("SIGNED", "已签收"),
    VERIFIED("VERIFIED", "已验证"),
    REJECTED("REJECTED", "已拒签"),
    EXCEPTION("EXCEPTION", "异常");

    private final String code;
    private final String name;

    SignatureStatus(String code, String name) {
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