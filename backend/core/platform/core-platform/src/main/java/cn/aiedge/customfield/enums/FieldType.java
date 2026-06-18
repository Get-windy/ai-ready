package cn.aiedge.customfield.enums;

public enum FieldType {
    STRING("STRING", "字符串"),
    INTEGER("INTEGER", "整数"),
    LONG("LONG", "长整数"),
    DOUBLE("DOUBLE", "浮点数"),
    BOOLEAN("BOOLEAN", "布尔值"),
    DATE("DATE", "日期"),
    DATETIME("DATETIME", "日期时间"),
    TEXT("TEXT", "长文本"),
    SELECTION("SELECTION", "选择字段"),
    MULTI_SELECTION("MULTI_SELECTION", "多选字段"),
    REFERENCE("REFERENCE", "关联字段"),
    JSON("JSON", "JSON对象"),
    FILE("FILE", "文件"),
    IMAGE("IMAGE", "图片");

    private final String code;
    private final String name;

    FieldType(String code, String name) {
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