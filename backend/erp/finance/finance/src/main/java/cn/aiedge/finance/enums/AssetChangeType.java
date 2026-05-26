package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AssetChangeType {
    
    ACQUISITION(1, "资产购置"),
    TRANSFER(2, "资产转移"),
    DISPOSE(3, "资产处置"),
    SCRAP(4, "资产报废"),
    DEPRECIATION(5, "折旧计提"),
    REVALUATION(6, "资产重估"),
    MODIFY(7, "信息修改");
    
    private final Integer code;
    private final String name;
    
    public static AssetChangeType fromCode(Integer code) {
        for (AssetChangeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}