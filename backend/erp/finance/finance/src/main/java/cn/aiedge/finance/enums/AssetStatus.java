package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AssetStatus {
    
    NORMAL(1, "正常使用"),
    IDLE(2, "闲置"),
    MAINTENANCE(3, "维修中"),
    DISPOSED(4, "已处置"),
    SCRAPPED(5, "已报废"),
    TRANSFERRED(6, "已转移");
    
    private final Integer code;
    private final String name;
    
    public static AssetStatus fromCode(Integer code) {
        for (AssetStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}