package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportStatus {
    
    DRAFT(0, "草稿"),
    GENERATED(1, "已生成"),
    APPROVED(2, "已审核"),
    PUBLISHED(3, "已发布");
    
    private final Integer code;
    private final String name;
    
    public static ReportStatus fromCode(Integer code) {
        for (ReportStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}