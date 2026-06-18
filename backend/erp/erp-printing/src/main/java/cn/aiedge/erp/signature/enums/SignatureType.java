package cn.aiedge.erp.signature.enums;

public enum SignatureType {
    PHOTO("PHOTO", "拍照签收"),
    ELECTRONIC("ELECTRONIC", "电子签名"),
    FINGERPRINT("FINGERPRINT", "指纹签收"),
    CODE("CODE", "验证码签收");

    private final String code;
    private final String name;

    SignatureType(String code, String name) {
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