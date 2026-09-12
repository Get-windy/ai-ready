package cn.aiedge.erp.finance.expensedoc.service;

import cn.aiedge.erp.finance.expensedoc.dto.ApprovalProcessDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ApprovalSubmitDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApprovalDetailVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApprovalQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApproverConfigDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApproverSuggestionVO;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseApproval;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseDoc;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 费用审批 Service接口
 * 费用单多级审批流（部门→财务→总经理）：复用费用单状态机，审批记录落 erp_expense_approval。
 */
public interface ExpenseApprovalService {

    /** 待审批分页查询（只查审批中的费用单，支持「仅看我的待办」审批人隔离） */
    Page<ExpenseDoc> pagePending(ExpenseApprovalQuery query, Long loginUserId);

    /** 提交审批（草稿/已驳回 → 审批中，进入第一级审批队列） */
    ExpenseDoc submit(ApprovalSubmitDTO dto, Long operatorId, String operatorName);

    /** 审批处理（通过/驳回） */
    ExpenseDoc process(ApprovalProcessDTO dto, Long operatorId, String operatorName);

    /** 审批记录（按时间正序） */
    List<ExpenseApproval> records(Long docId);

    /** 审批详情（费用单 + 费用项明细 + 审批记录） */
    ExpenseApprovalDetailVO detail(Long docId);

    /** 审批人配置（按级别默认审批人） */
    ExpenseApproverConfigDTO getApproverConfig();

    /** 保存审批人配置（null 表示清除该级别配置） */
    void saveApproverConfig(ExpenseApproverConfigDTO dto);

    /**
     * 自动指派建议：按级别返回建议审批人
     * 第一级优先取费用单所属部门的负责人，未设置时回退审批人配置；第二/三级取审批人配置。
     */
    List<ExpenseApproverSuggestionVO> suggestApprovers(Long docId, Integer totalLevel);
}
