package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 明细账科目分类树节点
 *
 * 结构：全部 / 资产类 / 负债类 / 权益类 / 成本类 / 损益类，组内按科目层级展开。
 * id 约定：'0'=全部；'type_{1..5}'=科目类型；其余为科目ID。
 */
@Data
public class LedgerSubjectTreeNodeDTO {

    /** 节点ID */
    private String id;

    /** 节点名称（CategoryListLayout 以 categoryName 作为标题字段） */
    private String categoryName;

    /** 科目ID（选中具体科目时有值） */
    private Long subjectId;

    /** 科目类型（选中「XX类」时有值） */
    private Integer subjectType;

    /** 子节点 */
    private List<LedgerSubjectTreeNodeDTO> children = new ArrayList<>();
}
