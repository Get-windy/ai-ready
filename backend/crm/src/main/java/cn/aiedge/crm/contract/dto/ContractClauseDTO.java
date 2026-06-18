package cn.aiedge.crm.contract.dto;

import lombok.Data;

@Data
public class ContractClauseDTO {

    private String clauseTitle;

    private String clauseContent;

    private Integer clauseType;

    private Boolean mandatory;

    private Boolean editable;

    private Integer sortOrder;

    private String remark;
}