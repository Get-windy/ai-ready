package cn.aiedge.erp.printing.enums;

import lombok.Getter;

@Getter
public enum ChainStatus {
    ACTIVE("ACTIVE", "启用"),
    DISABLED("DISABLED", "禁用");

    private final String code;
    private final String name;

    ChainStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
