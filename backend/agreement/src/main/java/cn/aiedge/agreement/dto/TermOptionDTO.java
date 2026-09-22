package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 条款字典选项的新增 / 修改入参（平台侧维护）。
 *
 * <p>⚠️ **没有 defaultOption 字段，也不许加**：平台给默认值 = 平台替双方决定责任划分
 * （㉜ / §3.4.4d1）。字典只回答"有哪些选项、各自什么含义"。</p>
 */
@Data
public class TermOptionDTO {

    /** 条款类别（同类别的选项在协议详情页归到同一个下拉） */
    private String termCode;

    /** 选项编码；留空由服务端按 term_code + 序号生成 */
    private String optionCode;

    private String optionLabel;

    /** 语义说明：选了它系统会怎么执行（必填，这段文字是给双方看的条款说明书） */
    private String semantics;

    /** 需要附带参数时填参数名（如"分账比例"），否则为空 */
    private String needsParam;

    private Boolean required;

    /** 法务审核结论：PENDING / APPROVED / REJECTED（不传按 PENDING 处理） */
    private String legalReviewStatus;

    private Integer sort;

    /** 1=启用 / 0=停用（前端表单默认 1） */
    private Integer status;
}
