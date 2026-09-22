package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 条款字典按类别分组后的一组（协议详情页一个下拉 = 一组）。
 *
 * <p>{@code required} 是该组的整体必填标记：组内**任一**启用选项标了 required 即为 true
 * （与前端 {@code isRequiredGroup} 同口径）。必填组没选完，协议不许置生效。</p>
 */
@Data
public class TermOptionGroupVO {

    private String termCode;

    /** 类别中文名（取组内第一个选项的名称作为展示名） */
    private String termName;

    private Boolean required;

    private List<TermOptionVO> options;
}
