package cn.aiedge.erp.printing.enums;

import lombok.Getter;

@Getter
public enum TemplateStatus {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String name;

    TemplateStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
