package cn.aiedge.erp.expense.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeApprovalRecordVO;
import cn.aiedge.erp.expense.dto.FeeApprovalRequest;
import cn.aiedge.erp.expense.entity.FeeApplication;
import cn.aiedge.erp.expense.entity.FeeApprovalRecord;
import cn.aiedge.erp.expense.entity.FeeReimbursement;
import cn.aiedge.erp.expense.mapper.FeeApplicationMapper;
import cn.aiedge.erp.expense.mapper.FeeApprovalRecordMapper;
import cn.aiedge.erp.expense.mapper.FeeReimbursementMapper;
import cn.aiedge.erp.expense.service.FeeApprovalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeApprovalServiceImpl implements FeeApprovalService {

    private final FeeApprovalRecordMapper approvalRecordMapper;
    private final FeeApplicationMapper applicationMapper;
    private final FeeReimbursementMapper reimbursementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processApproval(FeeApprovalRequest request, Long operatorId, String operatorName) {
        String action = request.getAction();
        String businessType = request.getBusinessType();
        Long businessId = request.getBusinessId();

        // 查找当前业务对象
        String currentStatus;
        if ("APPLICATION".equals(businessType)) {
            FeeApplication app = applicationMapper.selectById(businessId);
            if (app == null) throw BusinessException.notFound("费用申请不存在");
            currentStatus = app.getStatus();
            if (!"SUBMITTED".equals(currentStatus) && !"APPROVING".equals(currentStatus)) {
                throw BusinessException.badRequest("当前状态不允许审批");
            }
        } else if ("REIMBURSEMENT".equals(businessType)) {
            FeeReimbursement reimb = reimbursementMapper.selectById(businessId);
            if (reimb == null) throw BusinessException.notFound("费用报销不存在");
            currentStatus = reimb.getStatus();
            if (!"SUBMITTED".equals(currentStatus) && !"APPROVING".equals(currentStatus)) {
                throw BusinessException.badRequest("当前状态不允许审批");
            }
        } else {
            throw BusinessException.badRequest("不支持的业务类型: " + businessType);
        }

        // 确定新状态
        String newStatus;
        switch (action) {
            case "APPROVE":
                newStatus = "APPROVED";
                break;
            case "REJECT":
                newStatus = "REJECTED";
                break;
            case "RETURN":
                newStatus = "SUBMITTED";
                break;
            case "TRANSFER":
                newStatus = currentStatus; // 转交不改变状态
                break;
            default:
                throw BusinessException.badRequest("不支持的审批动作: " + action);
        }

        // 更新业务对象状态
        if (!"TRANSFER".equals(action)) {
            if ("APPLICATION".equals(businessType)) {
                FeeApplication app = new FeeApplication();
                app.setId(businessId);
                app.setStatus(newStatus);
                if ("REJECTED".equals(newStatus)) {
                    app.setRejectReason(request.getComment());
                }
                applicationMapper.updateById(app);
            } else {
                FeeReimbursement reimb = new FeeReimbursement();
                reimb.setId(businessId);
                reimb.setStatus(newStatus);
                if ("REJECTED".equals(newStatus)) {
                    reimb.setRejectReason(request.getComment());
                }
                reimbursementMapper.updateById(reimb);
            }
        }

        // 创建审批记录
        FeeApprovalRecord record = new FeeApprovalRecord();
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setApprovalLevel(1);
        record.setApproverId(operatorId);
        record.setApproverName(operatorName);
        record.setApprovalAction(action);
        record.setApprovalComment(request.getComment());
        record.setApprovalTime(LocalDateTime.now());
        record.setAssigneeId(request.getAssigneeId());
        record.setAssigneeName(request.getAssigneeName());
        record.setPreviousStatus(currentStatus);
        record.setCurrentStatus(newStatus);
        record.setCreateBy(operatorId);
        record.setUpdateBy(operatorId);
        approvalRecordMapper.insert(record);

        log.info("审批操作完成: type={}, bizId={}, action={}, operator={}", businessType, businessId, action, operatorName);
    }

    @Override
    public List<FeeApprovalRecordVO> getApprovalRecords(String businessType, Long businessId) {
        LambdaQueryWrapper<FeeApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeApprovalRecord::getBusinessType, businessType)
               .eq(FeeApprovalRecord::getBusinessId, businessId)
               .orderByAsc(FeeApprovalRecord::getApprovalLevel)
               .orderByAsc(FeeApprovalRecord::getApprovalTime);

        return approvalRecordMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<FeeApprovalRecordVO> getPendingApprovals(Long approverId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<FeeApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeApprovalRecord::getApproverId, approverId)
               .eq(FeeApprovalRecord::getApprovalAction, "SUBMIT")
               .orderByDesc(FeeApprovalRecord::getApprovalTime);

        Page<FeeApprovalRecord> page = new Page<>(pageNum, pageSize);
        Page<FeeApprovalRecord> result = approvalRecordMapper.selectPage(page, wrapper);

        List<FeeApprovalRecordVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), pageNum, pageSize);
    }

    private FeeApprovalRecordVO convertToVO(FeeApprovalRecord record) {
        FeeApprovalRecordVO vo = new FeeApprovalRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
