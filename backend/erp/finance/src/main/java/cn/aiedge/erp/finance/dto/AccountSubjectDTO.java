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
    private Integer level;
    private Integer subjectType;
    private Integer direction;
    private Boolean isLeaf;
    private Boolean isEnabled;
    private String remark;
    private List<AccountSubjectDTO> children;
}
