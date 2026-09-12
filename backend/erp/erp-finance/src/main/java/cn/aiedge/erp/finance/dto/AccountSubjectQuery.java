package cn.aiedge.erp.finance.dto;

import lombok.Data;

/**
 * 会计科目查询条件（资料 → 财务账户 → 会计科目）
 */
@Data
public class AccountSubjectQuery {

    /** 科目名称/编号/助记码 模糊匹配 */
    private String keyword;

    /** 科目分类：1-资产 2-负债 3-权益 4-成本 5-损益 */
    private Integer subjectType;

    /** 借贷方向：1-借方 2-贷方（缺省不过滤，供「其他收入」等收入类科目视图使用） */
    private Integer direction;

    /** 上级科目ID */
    private Long parentId;

    /**
     * 显示停用（true=含已停用科目）。
     * 缺省 true，保持凭证/账簿等既有调用方「取全量科目」的行为不变。
     */
    private Boolean includeDisabled;

    /** 显示层次结构（true=树形，false=平铺） */
    private Boolean hierarchical;

    public boolean includeDisabledOrDefault() {
        return includeDisabled == null || Boolean.TRUE.equals(includeDisabled);
    }

    public boolean hierarchicalOrDefault() {
        return hierarchical == null || Boolean.TRUE.equals(hierarchical);
    }
}
