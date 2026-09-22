package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 保存草稿（{@code PUT /api/agreement/{id}}）的回执。
 *
 * <p>契约要求"响应里要能看出**哪些必填项还没选**"：用户点一次保存就该知道
 * 距离"能生效"还差什么，而不是点生效才被拒。</p>
 *
 * <p>⚠️ 这里只做**提示**，不做拦截：草稿阶段本来就允许必填项未齐（正是"洽谈中"的含义）。
 * 真正拦住生效的是 {@code POST /version/{id}/activate}。</p>
 */
@Data
public class AgreementSaveResultVO {

    private Long id;

    /** 本次写入条款的草稿版本 ID */
    private Long versionId;

    /** 缺失的必填条款类别编码 */
    private List<String> missingRequiredTerms;

    /** 缺失的必填条款类别名称（给用户看的话术） */
    private List<String> missingRequiredTermNames;

    /** 已选条款里"需要参数但参数没填"的项 */
    private List<String> missingParams;

    /** 现在是否可以置为生效（缺任一必填/参数/双签都为 false；双签另由版本接口判断） */
    private Boolean readyToActivate;
}
