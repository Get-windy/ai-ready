package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.util.List;

/**
 * 预算编制批量操作请求
 */
@Data
public class BudgetBatchOpRequest {

    private List<Long> ids;

    private Long auditorId;

    private String auditorName;

    private String auditRemark;
}
