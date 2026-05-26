package cn.aiedge.erp.purchase.dto;

import cn.aiedge.erp.purchase.entity.PurchaseDemand;
import cn.aiedge.erp.purchase.enums.DemandPriority;
import cn.aiedge.erp.purchase.enums.DemandStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 采购需求响应DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseDemandResponseDTO {
    
    /**
     * 需求ID
     */
    private Long id;
    
    /**
     * 需求编号
     */
    private String demandNo;
    
    /**
     * 需求标题
     */
    private String title;
    
    /**
     * 需求描述
     */
    private String description;
    
    /**
     * 物料ID
     */
    private Long materialId;
    
    /**
     * 物料编码
     */
    private String materialCode;
    
    /**
     * 物料名称
     */
    private String materialName;
    
    /**
     * 需求数量
     */
    private BigDecimal demandQuantity;
    
    /**
     * 计量单位
     */
    private String unit;
    
    /**
     * 需求日期
     */
    private LocalDateTime demandDate;
    
    /**
     * 期望到货日期
     */
    private LocalDateTime expectedDeliveryDate;
    
    /**
     * 建议单价
     */
    private BigDecimal suggestedUnitPrice;
    
    /**
     * 预算金额
     */
    private BigDecimal budgetAmount;
    
    /**
     * 需求部门ID
     */
    private Long departmentId;
    
    /**
     * 需求部门名称
     */
    private String departmentName;
    
    /**
     * 需求人ID
     */
    private Long demandUserId;
    
    /**
     * 需求人姓名
     */
    private String demandUserName;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 优先级描述
     */
    private String priorityDesc;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 审批状态
     */
    private Integer approvalStatus;
    
    /**
     * 审批状态描述
     */
    private String approvalStatusDesc;
    
    /**
     * 审批人ID
     */
    private Long approvedBy;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvedTime;
    
    /**
     * 审批意见
     */
    private String approvalComment;
    
    /**
     * 关联的询价单ID
     */
    private Long inquiryId;
    
    /**
     * 关联的采购订单ID
     */
    private Long orderId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 扩展字段（JSON格式存储额外信息）
     */
    private String extInfo;
    
    /**
     * 剩余天数（距期望到货日期）
     */
    private Long remainingDays;
    
    /**
     * 是否紧急（剩余天数<7天且优先级高）
     */
    private Boolean urgent;
    
    /**
     * 需求紧迫性等级（1-正常，2-较急，3-紧急）
     */
    private Integer urgencyLevel;
    
    /**
     * 创建人ID
     */
    private Long createBy;
    
    /**
     * 更新人ID
     */
    private Long updateBy;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 从实体转换为响应DTO
     */
    public static PurchaseDemandResponseDTO fromEntity(PurchaseDemand demand) {
        PurchaseDemandResponseDTO dto = new PurchaseDemandResponseDTO();
        
        dto.setId(demand.getId());
        dto.setDemandNo(demand.getDemandNo());
        dto.setTitle(demand.getTitle());
        dto.setDescription(demand.getDescription());
        dto.setMaterialId(demand.getMaterialId());
        dto.setMaterialCode(demand.getMaterialCode());
        dto.setMaterialName(demand.getMaterialName());
        dto.setDemandQuantity(demand.getDemandQuantity());
        dto.setUnit(demand.getUnit());
        dto.setDemandDate(demand.getDemandDate());
        dto.setExpectedDeliveryDate(demand.getExpectedDeliveryDate());
        dto.setSuggestedUnitPrice(demand.getSuggestedUnitPrice());
        dto.setBudgetAmount(demand.getBudgetAmount());
        dto.setDepartmentId(demand.getDepartmentId());
        dto.setDepartmentName(demand.getDepartmentName());
        dto.setDemandUserId(demand.getDemandUserId());
        dto.setDemandUserName(demand.getDemandUserName());
        dto.setPriority(demand.getPriority());
        dto.setPriorityDesc(DemandPriority.fromValue(demand.getPriority()).getDescription());
        dto.setStatus(demand.getStatus());
        dto.setStatusDesc(DemandStatus.fromValue(demand.getStatus()).getDescription());
        dto.setApprovalStatus(demand.getApprovalStatus());
        dto.setApprovedBy(demand.getApprovedBy());
        dto.setApprovedTime(demand.getApprovedTime());
        dto.setApprovalComment(demand.getApprovalComment());
        dto.setInquiryId(demand.getInquiryId());
        dto.setOrderId(demand.getOrderId());
        dto.setCreateTime(demand.getCreateTime());
        dto.setUpdateTime(demand.getUpdateTime());
        dto.setRemark(demand.getRemark());
        dto.setExtInfo(demand.getExtInfo());
        dto.setCreateBy(demand.getCreateBy());
        dto.setUpdateBy(demand.getUpdateBy());
        dto.setVersion(demand.getVersion());
        
        // 计算剩余天数和紧迫性
        LocalDateTime now = LocalDateTime.now();
        if (demand.getExpectedDeliveryDate() != null) {
            long days = ChronoUnit.DAYS.between(now, demand.getExpectedDeliveryDate());
            dto.setRemainingDays(days);
            
            // 计算紧迫性等级
            if (days < 7 && demand.getPriority() >= 3) { // 高优先级且剩余时间短
                dto.setUrgent(true);
                dto.setUrgencyLevel(3); // 紧急
            } else if (days < 14) {
                dto.setUrgent(false);
                dto.setUrgencyLevel(2); // 较急
            } else {
                dto.setUrgent(false);
                dto.setUrgencyLevel(1); // 正常
            }
        }
        
        return dto;
    }
}