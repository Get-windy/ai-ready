package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountLevel {
    
    LEVEL_1(1, "一级科目"),
    LEVEL_2(2, "二级科目"),
    LEVEL_3(3, "三级科目"),
    LEVEL_4(4, "四级科目"),
    LEVEL_5(5, "五级科目");
    
    private final Integer code;
    private final String name;
    
    public static AccountLevel fromCode(Integer code) {
        for (AccountLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}