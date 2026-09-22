package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 条款字典选项（下发形态）。⚠️ 刻意没有 defaultValue 字段 —— 见 {@code TermOptionDTO}。
 */
@Data
public class TermOptionVO {

    private Long id;

    private String termCode;

    private String termName;

    private String optionCode;

    private String optionLabel;

    private String semantics;

    private String needsParam;

    private Boolean required;

    private String legalReviewStatus;

    private Integer sort;

    /** 1=启用 / 0=停用 */
    private Integer status;
}
