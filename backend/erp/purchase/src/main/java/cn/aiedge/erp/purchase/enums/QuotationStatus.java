package cn.aiedge.erp.purchase.enums;

import lombok.Getter;

/**
 * 报价状态枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum QuotationStatus {
    
    /**
     * 草稿
     * - 报价正在准备中
     * - 供应商内部草稿
     */
    DRAFT("DRAFT", "草稿"),
    
    /**
     * 已提交
     * - 供应商已提交报价
     * - 等待采购方评审
     */
    SUBMITTED("SUBMITTED", "已提交"),
    
    /**
     * 评审中
     * - 采购方正在评审报价
     * - 多部门协同评审
     */
    UNDER_REVIEW("UNDER_REVIEW", "评审中"),
    
    /**
     * 技术评审
     * - 技术部门评审技术方案
     * - 评估技术可行性
     */
    TECHNICAL_REVIEW("TECHNICAL_REVIEW", "技术评审"),
    
    /**
     * 商务评审
     * - 商务部门评审价格条款
     * - 评估商务条件
     */
    COMMERCIAL_REVIEW("COMMERCIAL_REVIEW", "商务评审"),
    
    /**
     * 财务评审
     * - 财务部门评审付款条件
     * - 评估财务风险
     */
    FINANCIAL_REVIEW("FINANCIAL_REVIEW", "财务评审"),
    
    /**
     * 已接受
     * - 报价被采购方接受
     * - 等待转为订单
     */
    ACCEPTED("ACCEPTED", "已接受"),
    
    /**
     * 已拒绝
     * - 报价被采购方拒绝
     * - 需要记录拒绝原因
     */
    REJECTED("REJECTED", "已拒绝"),
    
    /**
     * 谈判中
     * - 正在进行价格谈判
     * - 双方协商修改条款
     */
    NEGOTIATING("NEGOTIATING", "谈判中"),
    
    /**
     * 已修改
     * - 报价已根据反馈修改
     * - 重新提交评审
     */
    MODIFIED("MODIFIED", "已修改"),
    
    /**
     * 已过期
     * - 报价有效期已过
     * - 需要重新报价
     */
    EXPIRED("EXPIRED", "已过期"),
    
    /**
     * 已撤回
     * - 供应商撤回报价
     * - 采购方不再考虑
     */
    WITHDRAWN("WITHDRAWN", "已撤回"),
    
    /**
     * 已转为订单
     * - 报价已转为采购订单
     * - 进入采购执行阶段
     */
    CONVERTED_TO_ORDER("CONVERTED_TO_ORDER", "已转为订单"),
    
    /**
     * 备选
     * - 报价作为备选方案
     * - 主选方案有问题时启用
     */
    RESERVE("RESERVE", "备选"),
    
    /**
     * 需要澄清
     * - 报价信息需要澄清
     * - 供应商需要补充说明
     */
    NEED_CLARIFICATION("NEED_CLARIFICATION", "需要澄清"),
    
    /**
     * 等待补充资料
     * - 需要供应商补充资料
     * - 资料齐全后继续评审
     */
    WAITING_FOR_DOCUMENTS("WAITING_FOR_DOCUMENTS", "等待补充资料"),
    
    /**
     * 已存档
     * - 报价流程结束
     * - 报价信息存档
     */
    ARCHIVED("ARCHIVED", "已存档");
    
    private final String code;
    private final String description;
    
    QuotationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static QuotationStatus fromCode(String code) {
        for (QuotationStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }
    
    /**
     * 判断是否为进行中状态
     */
    public boolean isInProgress() {
        return this == SUBMITTED || 
               this == UNDER_REVIEW || 
               this == TECHNICAL_REVIEW || 
               this == COMMERCIAL_REVIEW || 
               this == FINANCIAL_REVIEW || 
               this == NEGOTIATING || 
               this == MODIFIED || 
               this == NEED_CLARIFICATION || 
               this == WAITING_FOR_DOCUMENTS;
    }
    
    /**
     * 判断是否为终态
     */
    public boolean isTerminal() {
        return this == ACCEPTED || 
               this == REJECTED || 
               this == EXPIRED || 
               this == WITHDRAWN || 
               this == CONVERTED_TO_ORDER || 
               this == ARCHIVED;
    }
    
    /**
     * 判断是否为评审状态
     */
    public boolean isReviewStatus() {
        return this == UNDER_REVIEW || 
               this == TECHNICAL_REVIEW || 
               this == COMMERCIAL_REVIEW || 
               this == FINANCIAL_REVIEW;
    }
    
    /**
     * 判断是否可转为订单
     */
    public boolean canConvertToOrder() {
        return this == ACCEPTED || this == RESERVE;
    }
    
    /**
     * 判断是否需要供应商行动
     */
    public boolean needsSupplierAction() {
        return this == NEGOTIATING || 
               this == NEED_CLARIFICATION || 
               this == WAITING_FOR_DOCUMENTS;
    }
    
    /**
     * 判断是否需要采购方行动
     */
    public boolean needsBuyerAction() {
        return this == SUBMITTED || 
               this == UNDER_REVIEW || 
               this == TECHNICAL_REVIEW || 
               this == COMMERCIAL_REVIEW || 
               this == FINANCIAL_REVIEW || 
               this == MODIFIED;
    }
    
    /**
     * 判断是否可以修改
     */
    public boolean canModify() {
        return this == DRAFT || 
               this == NEGOTIATING || 
               this == NEED_CLARIFICATION || 
               this == WAITING_FOR_DOCUMENTS;
    }
    
    /**
     * 判断是否可以撤回
     */
    public boolean canWithdraw() {
        return this == DRAFT || 
               this == SUBMITTED || 
               this.isReviewStatus() || 
               this == NEGOTIATING;
    }
    
    /**
     * 获取状态转换规则
     */
    public static boolean canTransition(QuotationStatus from, QuotationStatus to) {
        // 状态转换规则
        return switch (from) {
            case DRAFT -> to == SUBMITTED || to == WITHDRAWN;
            case SUBMITTED -> to == UNDER_REVIEW || to == REJECTED || to == EXPIRED || to == WITHDRAWN;
            case UNDER_REVIEW -> to == TECHNICAL_REVIEW || to == COMMERCIAL_REVIEW || to == FINANCIAL_REVIEW || to == ACCEPTED || to == REJECTED || to == NEGOTIATING || to == NEED_CLARIFICATION || to == WAITING_FOR_DOCUMENTS;
            case TECHNICAL_REVIEW -> to == COMMERCIAL_REVIEW || to == REJECTED || to == NEGOTIATING || to == NEED_CLARIFICATION;
            case COMMERCIAL_REVIEW -> to == FINANCIAL_REVIEW || to == REJECTED || to == NEGOTIATING || to == NEED_CLARIFICATION;
            case FINANCIAL_REVIEW -> to == ACCEPTED || to == REJECTED || to == NEGOTIATING || to == NEED_CLARIFICATION;
            case ACCEPTED -> to == CONVERTED_TO_ORDER || to == REJECTED || to == RESERVE || to == ARCHIVED;
            case REJECTED -> to == ARCHIVED;
            case NEGOTIATING -> to == MODIFIED || to == ACCEPTED || to == REJECTED || to == WITHDRAWN;
            case MODIFIED -> to == UNDER_REVIEW || to == REJECTED || to == WITHDRAWN;
            case EXPIRED -> to == ARCHIVED;
            case WITHDRAWN -> to == ARCHIVED;
            case CONVERTED_TO_ORDER -> to == ARCHIVED;
            case RESERVE -> to == ACCEPTED || to == REJECTED || to == ARCHIVED;
            case NEED_CLARIFICATION -> to == MODIFIED || to == UNDER_REVIEW || to == REJECTED || to == WITHDRAWN;
            case WAITING_FOR_DOCUMENTS -> to == UNDER_REVIEW || to == REJECTED || to == WITHDRAWN;
            case ARCHIVED -> false; // 终态不可转换
        };
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (this) {
            case DRAFT -> "报价正在准备中";
            case SUBMITTED -> "供应商已提交报价，等待评审";
            case UNDER_REVIEW -> "报价正在综合评审中";
            case TECHNICAL_REVIEW -> "技术部门正在评审技术方案";
            case COMMERCIAL_REVIEW -> "商务部门正在评审价格条款";
            case FINANCIAL_REVIEW -> "财务部门正在评审付款条件";
            case ACCEPTED -> "报价已被接受";
            case REJECTED -> "报价已被拒绝";
            case NEGOTIATING -> "正在进行价格谈判";
            case MODIFIED -> "报价已根据反馈修改";
            case EXPIRED -> "报价有效期已过";
            case WITHDRAWN -> "供应商已撤回报价";
            case CONVERTED_TO_ORDER -> "报价已转为采购订单";
            case RESERVE -> "报价作为备选方案";
            case NEED_CLARIFICATION -> "需要供应商澄清报价信息";
            case WAITING_FOR_DOCUMENTS -> "等待供应商补充资料";
            case ARCHIVED -> "报价已存档";
        };
    }
    
    /**
     * 获取状态颜色（用于UI显示）
     */
    public String getStatusColor() {
        return switch (this) {
            case DRAFT -> "gray";
            case SUBMITTED -> "blue";
            case UNDER_REVIEW -> "orange";
            case TECHNICAL_REVIEW -> "purple";
            case COMMERCIAL_REVIEW -> "cyan";
            case FINANCIAL_REVIEW -> "blue";
            case ACCEPTED -> "green";
            case REJECTED -> "red";
            case NEGOTIATING -> "yellow";
            case MODIFIED -> "blue";
            case EXPIRED -> "gray";
            case WITHDRAWN -> "red";
            case CONVERTED_TO_ORDER -> "green";
            case RESERVE -> "orange";
            case NEED_CLARIFICATION -> "orange";
            case WAITING_FOR_DOCUMENTS -> "orange";
            case ARCHIVED -> "gray";
        };
    }
    
    /**
     * 获取下一步建议操作
     */
    public String getSuggestedAction() {
        return switch (this) {
            case DRAFT -> "提交报价";
            case SUBMITTED -> "开始评审";
            case UNDER_REVIEW -> "分配评审部门";
            case TECHNICAL_REVIEW -> "完成技术评审";
            case COMMERCIAL_REVIEW -> "完成商务评审";
            case FINANCIAL_REVIEW -> "完成财务评审";
            case ACCEPTED -> "转为订单或设为备选";
            case REJECTED -> "存档并通知供应商";
            case NEGOTIATING -> "继续谈判或结束谈判";
            case MODIFIED -> "重新评审";
            case EXPIRED -> "存档或要求重新报价";
            case WITHDRAWN -> "存档并更新供应商记录";
            case CONVERTED_TO_ORDER -> "跟踪订单执行";
            case RESERVE -> "监控主选方案状态";
            case NEED_CLARIFICATION -> "要求供应商澄清";
            case WAITING_FOR_DOCUMENTS -> "提醒供应商补充资料";
            case ARCHIVED -> "无需操作";
        };
    }
}