package cn.aiedge.erp.printing.enums;

import lombok.Getter;

@Getter
public enum ClientStatus {
    ONLINE("ONLINE", "在线"),
    OFFLINE("OFFLINE", "离线"),
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String name;

    ClientStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
