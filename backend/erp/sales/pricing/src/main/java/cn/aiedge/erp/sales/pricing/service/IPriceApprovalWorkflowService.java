package cn.aiedge.erp.sales.pricing.service;

import cn.aiedge.erp.sales.pricing.dto.PriceApprovalRequestDTO;
import cn.aiedge.erp.sales.pricing.dto.PriceApprovalResponseDTO;
import cn.aiedge.erp.sales.pricing.dto.PriceApprovalSearchDTO;
import cn.aiedge.erp.sales.pricing.entity.PriceApprovalRecord;
import cn.aiedge.erp.sales.pricing.entity.PriceSpecialApproval;
import cn.aiedge.erp.sales.pricing.enums.PriceApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格审批工作流服务接口
 * 管理价格特批申请的完整审批流程
 */
public interface IPriceApprovalWorkflowService {
    
    // ========== 申请管理 ==========
    
    /**
     * 创建价格特批申请
     * 
     * @param requestDTO 申请请求数据
     * @return 创建的特批申请
     */
    PriceSpecialApproval createApprovalRequest(PriceApprovalRequestDTO requestDTO);
    
    /**
     * 提交价格特批申请
     * 
     * @param approvalId 申请ID
     * @param submitterId 提交人ID
     * @return 提交后的申请
     */
    PriceSpecialApproval submitApprovalRequest(Long approvalId, Long submitterId);
    
    /**
     * 更新价格特批申请
     * 
     * @param approvalId 申请ID
     * @param requestDTO 更新请求数据
     * @return 更新后的申请
     */
    PriceSpecialApproval updateApprovalRequest(Long approvalId, PriceApprovalRequestDTO requestDTO);
    
    /**
     * 撤回价格特批申请
     * 
     * @param approvalId 申请ID
     * @param operatorId 操作人ID
     * @param reason 撤回原因
     * @return 撤回后的申请
     */
    PriceSpecialApproval withdrawApprovalRequest(Long approvalId, Long operatorId, String reason);
    
    /**
     * 取消价格特批申请
     * 
     * @param approvalId 申请ID
     * @param operatorId 操作人ID
     * @param reason 取消原因
     * @return 取消后的申请
     */
    PriceSpecialApproval cancelApprovalRequest(Long approvalId, Long operatorId, String reason);
    
    // ========== 审批操作 ==========
    
    /**
     * 审批通过
     * 
     * @param approvalId 申请ID
     * @param approverId 审批人ID
     * @param comments 审批意见
     * @return 审批后的申请
     */
    PriceSpecialApproval approve(Long approvalId, Long approverId, String comments);
    
    /**
     * 审批拒绝
     * 
     * @param approvalId 申请ID
     * @param approverId 审批人ID
     * @param rejectReason 拒绝原因
     * @param suggestions 修改建议
     * @return 审批后的申请
     */
    PriceSpecialApproval reject(Long approvalId, Long approverId, String rejectReason, String suggestions);
    
    /**
     * 有条件批准
     * 
     * @param approvalId 申请ID
     * @param approverId 审批人ID
     * @param conditions 批准条件
     * @param comments 审批意见
     * @return 审批后的申请
     */
    PriceSpecialApproval approveWithConditions(Long approvalId, Long approverId, String conditions, String comments);
    
    /**
     * 退回修改
     * 
     * @param approvalId 申请ID
     * @param approverId 审批人ID
     * @param reason 退回原因
     * @param requiredChanges 需要修改的内容
     * @return 退回后的申请
     */
    PriceSpecialApproval returnForModification(Long approvalId, Long approverId, String reason, String requiredChanges);
    
    // ========== 查询方法 ==========
    
    /**
     * 根据ID获取价格特批申请
     * 
     * @param approvalId 申请ID
     * @return 价格特批申请
     */
    PriceSpecialApproval getApprovalById(Long approvalId);
    
    /**
     * 根据申请编号获取价格特批申请
     * 
     * @param approvalCode 申请编号
     * @return 价格特批申请
     */
    PriceSpecialApproval getApprovalByCode(String approvalCode);
    
    /**
     * 分页查询价格特批申请
     * 
     * @param searchDTO 查询条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PriceSpecialApproval> searchApprovals(PriceApprovalSearchDTO searchDTO, Pageable pageable);
    
    /**
     * 查询待我审批的申请
     * 
     * @param approverId 审批人ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PriceSpecialApproval> getPendingApprovals(Long approverId, Pageable pageable);
    
    /**
     * 查询我提交的申请
     * 
     * @param applicantId 申请人ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PriceSpecialApproval> getMySubmittedApprovals(Long applicantId, Pageable pageable);
    
    /**
     * 查询已完成的审批申请
     * 
     * @param operatorId 操作人ID（申请人或审批人）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PriceSpecialApproval> getCompletedApprovals(Long operatorId, Pageable pageable);
    
    /**
     * 查询过期的审批申请
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PriceSpecialApproval> getExpiredApprovals(Pageable pageable);
    
    /**
     * 查询紧急审批申请
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<PriceSpecialApproval> getUrgentApprovals(Pageable pageable);
    
    // ========== 审批记录管理 ==========
    
    /**
     * 获取申请的审批记录
     * 
     * @param approvalId 申请ID
     * @return 审批记录列表
     */
    List<PriceApprovalRecord> getApprovalRecords(Long approvalId);
    
    /**
     * 获取申请的完整审批历史
     * 
     * @param approvalId 申请ID
     * @return 审批历史记录
     */
    PriceApprovalResponseDTO getApprovalHistory(Long approvalId);
    
    /**
     * 添加审批备注
     * 
     * @param approvalId 申请ID
     * @param operatorId 操作人ID
     * @param comments 备注内容
     * @return 添加的审批记录
     */
    PriceApprovalRecord addApprovalComment(Long approvalId, Long operatorId, String comments);
    
    // ========== 统计方法 ==========
    
    /**
     * 统计审批申请数量
     * 
     * @param searchDTO 查询条件
     * @return 申请数量统计
     */
    ApprovalStatisticsDTO getApprovalStatistics(PriceApprovalSearchDTO searchDTO);
    
    /**
     * 获取审批效率统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批效率统计
     */
    ApprovalEfficiencyDTO getApprovalEfficiency(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取审批人统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审批人统计
     */
    ApproverStatisticsDTO getApproverStatistics(LocalDateTime startTime, LocalDateTime endTime);
    
    // ========== 工作流管理 ==========
    
    /**
     * 重新分配审批人
     * 
     * @param approvalId 申请ID
     * @param approverId 原审批人ID
     * @param newApproverId 新审批人ID
     * @param reason 重新分配原因
     * @return 重新分配后的申请
     */
    PriceSpecialApproval reassignApprover(Long approvalId, Long approverId, Long newApproverId, String reason);
    
    /**
     * 升级审批级别
     * 
     * @param approvalId 申请ID
     * @param operatorId 操作人ID
     * @param reason 升级原因
     * @return 升级后的申请
     */
    PriceSpecialApproval escalateApproval(Long approvalId, Long operatorId, String reason);
    
    /**
     * 委托审批
     * 
     * @param approvalId 申请ID
     * @param approverId 原审批人ID
     * @param delegateId 被委托人ID
     * @param reason 委托原因
     * @return 委托后的申请
     */
    PriceSpecialApproval delegateApproval(Long approvalId, Long approverId, Long delegateId, String reason);
    
    /**
     * 检查审批流程状态
     * 
     * @param approvalId 申请ID
     * @return 流程状态检查结果
     */
    WorkflowStatusDTO checkWorkflowStatus(Long approvalId);
    
    /**
     * 验证审批流程完整性
     * 
     * @param approvalId 申请ID
     * @return 验证结果
     */
    WorkflowValidationResult validateWorkflow(Long approvalId);
    
    // ========== 通知和提醒 ==========
    
    /**
     * 发送审批通知
     * 
     * @param approvalId 申请ID
     * @param notificationType 通知类型
     * @return 是否发送成功
     */
    boolean sendApprovalNotification(Long approvalId, String notificationType);
    
    /**
     * 发送审批提醒
     * 
     * @param approvalId 申请ID
     * @param approverId 审批人ID
     * @return 是否发送成功
     */
    boolean sendApprovalReminder(Long approvalId, Long approverId);
    
    /**
     * 批量发送过期提醒
     * 
     * @return 发送成功的数量
     */
    int sendExpirationReminders();
    
    // ========== 批量操作 ==========
    
    /**
     * 批量审批通过
     * 
     * @param approvalIds 申请ID列表
     * @param approverId 审批人ID
     * @param comments 审批意见
     * @return 批量审批结果
     */
    BatchApprovalResult batchApprove(List<Long> approvalIds, Long approverId, String comments);
    
    /**
     * 批量审批拒绝
     * 
     * @param approvalIds 申请ID列表
     * @param approverId 审批人ID
     * @param rejectReason 拒绝原因
     * @return 批量审批结果
     */
    BatchApprovalResult batchReject(List<Long> approvalIds, Long approverId, String rejectReason);
    
    /**
     * 批量撤回申请
     * 
     * @param approvalIds 申请ID列表
     * @param operatorId 操作人ID
     * @param reason 撤回原因
     * @return 批量撤回结果
     */
    BatchApprovalResult batchWithdraw(List<Long> approvalIds, Long operatorId, String reason);
    
    // ========== DTO类 ==========
    
    /**
     * 审批统计DTO
     */
    class ApprovalStatisticsDTO {
        private Long totalCount;
        private Long pendingCount;
        private Long approvedCount;
        private Long rejectedCount;
        private Long withdrawnCount;
        private Long expiredCount;
        private Long urgentCount;
        private BigDecimal approvalRate;
        private BigDecimal averageApprovalTime;
        
        // 构造函数、getter、setter省略
    }
    
    /**
     * 审批效率DTO
     */
    class ApprovalEfficiencyDTO {
        private Long totalApprovals;
        private BigDecimal averageApprovalTime;
        private Long minApprovalTime;
        private Long maxApprovalTime;
        private Long overdueCount;
        private BigDecimal overdueRate;
        private Map<String, Long> approvalTimeDistribution;
        
        // 构造函数、getter、setter省略
    }
    
    /**
     * 审批人统计DTO
     */
    class ApproverStatisticsDTO {
        private Long totalApprovers;
        private Long activeApprovers;
        private Map<String, Long> approvalCountByApprover;
        private Map<String, BigDecimal> approvalRateByApprover;
        private Map<String, BigDecimal> averageApprovalTimeByApprover;
        
        // 构造函数、getter、setter省略
    }
    
    /**
     * 工作流状态DTO
     */
    class WorkflowStatusDTO {
        private PriceApprovalStatus currentStatus;
        private Integer currentLevel;
        private Integer totalLevels;
        private Long currentApproverId;
        private String currentApproverName;
        private LocalDateTime submittedAt;
        private LocalDateTime lastUpdatedAt;
        private Long durationMinutes;
        private boolean isTimeout;
        private LocalDateTime expectedCompletionTime;
        private List<String> nextActions;
        
        // 构造函数、getter、setter省略
    }
    
    /**
     * 工作流验证结果
     */
    class WorkflowValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private List<String> suggestions;
        
        // 构造函数、getter、setter省略
    }
    
    /**
     * 批量审批结果
     */
    class BatchApprovalResult {
        private int totalCount;
        private int successCount;
        private int failureCount;
        private List<Long> successfulIds;
        private Map<Long, String> failedReasons;
        
        // 构造函数、getter、setter省略
    }
}