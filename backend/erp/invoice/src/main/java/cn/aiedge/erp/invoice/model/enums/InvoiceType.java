package cn.aiedge.erp.invoice.model.enums;

/**
 * 发票类型枚举
 */
public enum InvoiceType {
    /**
     * 销售发票 - 向客户开具的发票
     */
    SALES_INVOICE("销售发票", "Sales invoice to customer", "SI", true),
    
    /**
     * 采购发票 - 从供应商收到的发票
     */
    PURCHASE_INVOICE("采购发票", "Purchase invoice from supplier", "PI", true),
    
    /**
     * 形式发票 - 报价单/预开发票
     */
    PROFORMA_INVOICE("形式发票", "Proforma invoice/Quotation", "PF", false),
    
    /**
     * 税务发票 - 正式税务发票
     */
    TAX_INVOICE("税务发票", "Official tax invoice", "TI", true),
    
    /**
     * 红字发票 - 冲红/退货发票
     */
    CREDIT_NOTE("红字发票", "Credit note/Return invoice", "CN", true),
    
    /**
     * 电子发票 - 电子发票
     */
    ELECTRONIC_INVOICE("电子发票", "Electronic invoice", "EI", true),
    
    /**
     * 普通发票 - 普通发票
     */
    REGULAR_INVOICE("普通发票", "Regular invoice", "RI", true),
    
    /**
     * 专用发票 - 增值税专用发票
     */
    SPECIAL_INVOICE("专用发票", "VAT special invoice", "SP", true),
    
    /**
     * 机动车发票 - 机动车销售发票
     */
    VEHICLE_INVOICE("机动车发票", "Vehicle sales invoice", "VI", true),
    
    /**
     * 二手车发票 - 二手车销售发票
     */
    USED_VEHICLE_INVOICE("二手车发票", "Used vehicle sales invoice", "UVI", true),
    
    /**
     * 出口发票 - 出口销售发票
     */
    EXPORT_INVOICE("出口发票", "Export sales invoice", "EX", true),
    
    /**
     * 进口发票 - 进口采购发票
     */
    IMPORT_INVOICE("进口发票", "Import purchase invoice", "IM", true);
    
    private final String chineseName;
    private final String description;
    private final String code;
    private final boolean taxable;
    
    InvoiceType(String chineseName, String description, String code, boolean taxable) {
        this.chineseName = chineseName;
        this.description = description;
        this.code = code;
        this.taxable = taxable;
    }
    
    public String getChineseName() {
        return chineseName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCode() {
        return code;
    }
    
    public boolean isTaxable() {
        return taxable;
    }
    
    /**
     * 判断是否为销售类发票
     */
    public boolean isSalesType() {
        return this == SALES_INVOICE || 
               this == TAX_INVOICE || 
               this == ELECTRONIC_INVOICE ||
               this == REGULAR_INVOICE ||
               this == SPECIAL_INVOICE ||
               this == VEHICLE_INVOICE ||
               this == USED_VEHICLE_INVOICE ||
               this == EXPORT_INVOICE;
    }
    
    /**
     * 判断是否为采购类发票
     */
    public boolean isPurchaseType() {
        return this == PURCHASE_INVOICE || 
               this == IMPORT_INVOICE;
    }
    
    /**
     * 判断是否为调整类发票
     */
    public boolean isAdjustmentType() {
        return this == CREDIT_NOTE;
    }
    
    /**
     * 判断是否为预开发票
     */
    public boolean isProformaType() {
        return this == PROFORMA_INVOICE;
    }
    
    /**
     * 判断是否需要税务登记号
     */
    public boolean requiresTaxRegistrationNumber() {
        return this == TAX_INVOICE || 
               this == SPECIAL_INVOICE ||
               this == ELECTRONIC_INVOICE;
    }
    
    /**
     * 判断是否需要QR码
     */
    public boolean requiresQRCode() {
        return this == TAX_INVOICE || 
               this == ELECTRONIC_INVOICE ||
               this == SPECIAL_INVOICE;
    }
    
    /**
     * 判断是否支持电子开票
     */
    public boolean supportsElectronicBilling() {
        return this == ELECTRONIC_INVOICE || 
               this == TAX_INVOICE ||
               this == SPECIAL_INVOICE;
    }
    
    /**
     * 获取销售类发票类型
     */
    public static InvoiceType[] getSalesTypes() {
        return new InvoiceType[] {
            SALES_INVOICE,
            TAX_INVOICE,
            ELECTRONIC_INVOICE,
            REGULAR_INVOICE,
            SPECIAL_INVOICE,
            VEHICLE_INVOICE,
            USED_VEHICLE_INVOICE,
            EXPORT_INVOICE
        };
    }
    
    /**
     * 获取采购类发票类型
     */
    public static InvoiceType[] getPurchaseTypes() {
        return new InvoiceType[] {
            PURCHASE_INVOICE,
            IMPORT_INVOICE
        };
    }
    
    /**
     * 根据代码获取发票类型
     */
    public static InvoiceType fromCode(String code) {
        for (InvoiceType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown invoice type code: " + code);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %s", chineseName, code, description);
    }
}