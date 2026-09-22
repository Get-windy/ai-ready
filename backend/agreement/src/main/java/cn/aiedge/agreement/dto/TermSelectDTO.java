package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 一条条款选择：从平台字典里选一个选项 + 可选参数。
 *
 * <p>刻意**没有**"自由文本条款"这种入口 —— 自由文本平台无法自动执行、
 * 司法举证也弱（§3.4.4d：必须是从枚举里选的选项）。</p>
 */
@Data
public class TermSelectDTO {

    /** 条款类别，如 RETURN_FREIGHT */
    private String termCode;

    /** 选中的选项编码（必须存在于平台字典且启用） */
    private String optionCode;

    /** 选项的附带参数（字典 needs_param 非空时必填） */
    private String paramValue;
}
