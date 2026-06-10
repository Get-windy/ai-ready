package cn.aiedge.erp.expense.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeApprovalRecordVO;
import cn.aiedge.erp.expense.dto.FeeApprovalRequest;

import java.util.List;

/**
 * 审批服务接口
 */
public interface FeeApprovalService {

    /**
     * 执行审批操作(通过/拒绝/退回/转交)
     */
    void processApproval(FeeApprovalRequest request, Long operatorId, String operatorName);

    /**
     * 获取业务单据的审批记录
     */
    List<FeeApprovalRecordVO> getApprovalRecords(String businessType, Long businessId);

    /**
     * 获取待审批列表
     */
    PageResult<FeeApprovalRecordVO> getPendingApprovals(Long approverId, Integer pageNum, Integer pageSize);
}
