package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会计科目DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSubjectDTO {
    private Long id;
    private String subjectCode;
    private String subjectName;
    private Long parentId;
    /** 上级科目编码（仅列表展示用，树形构建后回填） */
    private String parentCode;
    /** 上级科目名称（仅列表展示用，树形构建后回填） */
    private String parentName;
    private Integer level;
    private Integer subjectType;
    private Integer direction;
    private Boolean isLeaf;
    private Boolean isEnabled;
    /** 助记码 */
    private String mnemonicCode;
    /** 科目全名 */
    private String fullName;
    /** 核算项 = 辅助核算类型ID */
    private Long auxiliaryTypeId;
    /** 核算项名称（辅助核算类型名，如 客户/部门） */
    private String auxiliaryTypeName;
    private String remark;
    private List<AccountSubjectDTO> children;
}
