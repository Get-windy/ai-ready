package cn.aiedge.erp.expense.model.enumeration;

/**
 * 费用类型枚举
 */
public enum ExpenseType {
    /**
     * 差旅费
     */
    TRAVEL("差旅费", "TRA", 1, true),
    
    /**
     * 办公用品
     */
    OFFICE_SUPPLIES("办公用品", "OFF", 2, true),
    
    /**
     * 会议费
     */
    MEETING("会议费", "MET", 3, true),
    
    /**
     * 招待费
     */
    ENTERTAINMENT("招待费", "ENT", 4, true),
    
    /**
     * 交通费
     */
    TRANSPORTATION("交通费", "TRN", 5, true),
    
    /**
     * 通讯费
     */
    COMMUNICATION("通讯费", "COM", 6, true),
    
    /**
     * 培训费
     */
    TRAINING("培训费", "TRG", 7, true),
    
    /**
     * 咨询费
     */
    CONSULTING("咨询费", "CON", 8, true),
    
    /**
     * 广告费
     */
    ADVERTISING("广告费", "ADV", 9, true),
    
    /**
     * 研发费
     */
    R_D("研发费", "RND", 10, true),
    
    /**
     * 设备费
     */
    EQUIPMENT("设备费", "EQP", 11, true),
    
    /**
     * 维修费
     */
    MAINTENANCE("维修费", "MNT", 12, true),
    
    /**
     * 租赁费
     */
    RENTAL("租赁费", "RNT", 13, true),
    
    /**
     * 保险费
     */
    INSURANCE("保险费", "INS", 14, true),
    
    /**
     * 税费
     */
    TAX("税费", "TAX", 15, false),
    
    /**
     * 其他
     */
    OTHER("其他", "OTH", 99, true);
    
    private final String description;
    private final String code;
    private final int order;
    private final boolean requiresApproval;
    
    ExpenseType(String description, String code, int order, boolean requiresApproval) {
        this.description = description;
        this.code = code;
        this.order = order;
        this.requiresApproval = requiresApproval;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCode() {
        return code;
    }
    
    public int getOrder() {
        return order;
    }
    
    public boolean isRequiresApproval() {
        return requiresApproval;
    }
    
    /**
     * 根据code获取枚举
     */
    public static ExpenseType fromCode(String code) {
        for (ExpenseType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
    
    /**
     * 根据description获取枚举
     */
    public static ExpenseType fromDescription(String description) {
        for (ExpenseType type : values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return OTHER;
    }
    
    /**
     * 是否需要发票
     */
    public boolean requiresInvoice() {
        return this != TAX; // 税费通常不需要发票
    }
    
    /**
     * 是否需要预算控制
     */
    public boolean requiresBudgetControl() {
        return this != TAX && this != OTHER; // 税费和其他通常不需要预算控制
    }
    
    /**
     * 获取默认审批级别
     */
    public int getDefaultApprovalLevel() {
        switch (this) {
            case TRAVEL:
            case ENTERTAINMENT:
            case CONSULTING:
            case ADVERTISING:
            case EQUIPMENT:
                return 3; // 需要高级审批
            case R_D:
            case TRAINING:
                return 2; // 需要中级审批
            default:
                return 1; // 需要基础审批
        }
    }
    
    /**
     * 获取费用类型列表用于前端展示
     */
    public static String[] getAllDescriptions() {
        ExpenseType[] values = values();
        String[] descriptions = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            descriptions[i] = values[i].getDescription();
        }
        return descriptions;
    }
    
    /**
     * 获取费用类型代码列表
     */
    public static String[] getAllCodes() {
        ExpenseType[] values = values();
        String[] codes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            codes[i] = values[i].getCode();
        }
        return codes;
    }
}