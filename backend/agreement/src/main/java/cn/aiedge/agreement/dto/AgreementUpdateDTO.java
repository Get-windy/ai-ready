package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 修改协议草稿的请求体。
 *
 * <p><b>条款的唯一写入口</b>：{@code terms} 写到 {@code versionId} 指定的 **DRAFT** 版本上；
 * 不传 {@code versionId} 则写当前草稿版本。这样保证"条款永远写在版本里"，
 * 从而与"已生效版本只读"天然一致（对 ACTIVE 版本的写入会被服务层直接拒绝）。</p>
 *
 * <p>{@code terms} 是**整份覆盖**：本次未列出的条款类别视为"仍未约定"（不是"保持原值"），
 * 这样前端把某一项清空时语义是明确的。</p>
 */
@Data
public class AgreementUpdateDTO {

    private String title;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    /** 要写入条款的草稿版本 ID；为空则写当前草稿版本 */
    private Long versionId;

    /** 该版本的完整条款选择（整份覆盖） */
    private List<TermSelectDTO> terms;
}
