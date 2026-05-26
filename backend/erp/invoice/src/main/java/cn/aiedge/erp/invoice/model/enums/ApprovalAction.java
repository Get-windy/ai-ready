package cn.aiedge.erp.invoice.model.enums;

/**
 * 审批动作枚举
 */
public enum ApprovalAction {
    /**
     * 批准
     */
    APPROVE("批准"),
    
    /**
     * 拒绝
     */
    REJECT("拒绝"),
    
    /**
     * 退回修改
     */
    RETURN("退回修改"),
    
    /**
     * 转审
     */
    DELEGATE("转审"),
    
    /**
     * 撤销
     */
    CANCEL("撤销");
    
    private final String chineseName;
    
    ApprovalAction(String chineseName) {
        this.chineseName = chineseName;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    @Override
    public String toString() {
        return this.name() + " (" + chineseName + ")";
    }
}