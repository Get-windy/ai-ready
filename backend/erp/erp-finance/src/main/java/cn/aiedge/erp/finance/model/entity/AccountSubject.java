package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会计科目实体
 * 定义企业的会计科目体系，支持多级层次结构
 */
@Data
@TableName("finance_account_subject")
@EqualsAndHashCode(callSuper = true)
public class AccountSubject extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 科目编码
     */
    @TableField("subject_code")
    private String subjectCode;

    /**
     * 科目名称
     */
    @TableField("subject_name")
    private String subjectName;

    /**
     * 父级科目ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 科目层级 (1-4)
     */
    @TableField("level")
    private Integer level;

    /**
     * 科目类型
     * 1-资产 2-负债 3-权益 4-成本 5-损益
     */
    @TableField("subject_type")
    private Integer subjectType;

    /**
     * 借贷方向
     * 1-借方 2-贷方
     */
    @TableField("direction")
    private Integer direction;

    /**
     * 是否叶子节点
     */
    @TableField("is_leaf")
    private Boolean isLeaf;

    /**
     * 是否启用
     */
    @TableField("is_enabled")
    private Boolean isEnabled = true;
}
