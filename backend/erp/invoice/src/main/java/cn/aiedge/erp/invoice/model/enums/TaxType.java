package cn.aiedge.erp.invoice.model.enums;

import java.math.BigDecimal;

/**
 * 税务类型枚举
 * 支持中国税务系统的各种税种
 */
public enum TaxType {
    /**
     * 增值税 - 一般纳税人
     */
    VAT_GENERAL("增值税(一般纳税人)", "Value Added Tax (General)", "VAT", new BigDecimal("0.13")),
    
    /**
     * 增值税 - 小规模纳税人
     */
    VAT_SMALL("增值税(小规模纳税人)", "Value Added Tax (Small-scale)", "VAT_S", new BigDecimal("0.03")),
    
    /**
     * 增值税 - 简易征收
     */
    VAT_SIMPLE("增值税(简易征收)", "Value Added Tax (Simple)", "VAT_SIMP", new BigDecimal("0.05")),
    
    /**
     * 增值税 - 零税率
     */
    VAT_ZERO("增值税(零税率)", "Value Added Tax (Zero Rate)", "VAT_Z", BigDecimal.ZERO),
    
    /**
     * 增值税 - 免税
     */
    VAT_EXEMPT("增值税(免税)", "Value Added Tax (Exempt)", "VAT_E", BigDecimal.ZERO),
    
    /**
     * 企业所得税
     */
    CORPORATE_INCOME_TAX("企业所得税", "Corporate Income Tax", "CIT", new BigDecimal("0.25")),
    
    /**
     * 个人所得税
     */
    PERSONAL_INCOME_TAX("个人所得税", "Personal Income Tax", "PIT", new BigDecimal("0.20")),
    
    /**
     * 消费税
     */
    CONSUMPTION_TAX("消费税", "Consumption Tax", "CT", new BigDecimal("0.10")),
    
    /**
     * 城市维护建设税
     */
    URBAN_CONSTRUCTION_TAX("城市维护建设税", "Urban Construction Tax", "UCT", new BigDecimal("0.07")),
    
    /**
     * 教育费附加
     */
    EDUCATION_SURCHARGE("教育费附加", "Education Surcharge", "ES", new BigDecimal("0.03")),
    
    /**
     * 地方教育附加
     */
    LOCAL_EDUCATION_SURCHARGE("地方教育附加", "Local Education Surcharge", "LES", new BigDecimal("0.02")),
    
    /**
     * 印花税
     */
    STAMP_DUTY("印花税", "Stamp Duty", "SD", new BigDecimal("0.0005")),
    
    /**
     * 房产税
     */
    PROPERTY_TAX("房产税", "Property Tax", "PT", new BigDecimal("0.012")),
    
    /**
     * 土地使用税
     */
    LAND_USE_TAX("土地使用税", "Land Use Tax", "LUT", new BigDecimal("0.006")),
    
    /**
     * 车船税
     */
    VEHICLE_TAX("车船税", "Vehicle and Vessel Tax", "VT", new BigDecimal("0.10")),
    
    /**
     * 资源税
     */
    RESOURCE_TAX("资源税", "Resource Tax", "RT", new BigDecimal("0.02")),
    
    /**
     * 环境保护税
     */
    ENVIRONMENTAL_TAX("环境保护税", "Environmental Protection Tax", "EPT", new BigDecimal("0.01")),
    
    /**
     * 关税
     */
    CUSTOMS_DUTY("关税", "Customs Duty", "CD", new BigDecimal("0.10")),
    
    /**
     * 进口增值税
     */
    IMPORT_VAT("进口增值税", "Import VAT", "IVAT", new BigDecimal("0.13")),
    
    /**
     * 进口消费税
     */
    IMPORT_CONSUMPTION_TAX("进口消费税", "Import Consumption Tax", "ICT", new BigDecimal("0.10")),
    
    /**
     * 出口退税
     */
    EXPORT_REFUND("出口退税", "Export Tax Refund", "ETR", new BigDecimal("-0.13"));
    
    private final String chineseName;
    private final String description;
    private final String code;
    private final BigDecimal defaultRate;
    
    TaxType(String chineseName, String description, String code, BigDecimal defaultRate) {
        this.chineseName = chineseName;
        this.description = description;
        this.code = code;
        this.defaultRate = defaultRate;
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
    
    public BigDecimal getDefaultRate() {
        return defaultRate;
    }
    
    /**
     * 判断是否为增值税
     */
    public boolean isVAT() {
        return this.name().startsWith("VAT_");
    }
    
    /**
     * 判断是否为附加税
     */
    public boolean isSurcharge() {
        return this == URBAN_CONSTRUCTION_TAX || 
               this == EDUCATION_SURCHARGE || 
               this == LOCAL_EDUCATION_SURCHARGE;
    }
    
    /**
     * 判断是否为进口税
     */
    public boolean isImportTax() {
        return this == CUSTOMS_DUTY || 
               this == IMPORT_VAT || 
               this == IMPORT_CONSUMPTION_TAX;
    }
    
    /**
     * 判断是否为出口税
     */
    public boolean isExportTax() {
        return this == EXPORT_REFUND;
    }
    
    /**
     * 判断是否为财产税
     */
    public boolean isPropertyTax() {
        return this == PROPERTY_TAX || 
               this == LAND_USE_TAX || 
               this == VEHICLE_TAX;
    }
    
    /**
     * 判断是否为环境税
     */
    public boolean isEnvironmentalTax() {
        return this == ENVIRONMENTAL_TAX || 
               this == RESOURCE_TAX;
    }
    
    /**
     * 判断是否为零税率
     */
    public boolean isZeroRate() {
        return this == VAT_ZERO;
    }
    
    /**
     * 判断是否为免税
     */
    public boolean isExempt() {
        return this == VAT_EXEMPT;
    }
    
    /**
     * 判断是否为退税
     */
    public boolean isRefund() {
        return this == EXPORT_REFUND;
    }
    
    /**
     * 判断是否支持抵扣
     */
    public boolean isDeductible() {
        return isVAT() && !isZeroRate() && !isExempt() && !isRefund();
    }
    
    /**
     * 获取增值税类型
     */
    public static TaxType[] getVATTypes() {
        return new TaxType[] {
            VAT_GENERAL,
            VAT_SMALL,
            VAT_SIMPLE,
            VAT_ZERO,
            VAT_EXEMPT
        };
    }
    
    /**
     * 获取附加税类型
     */
    public static TaxType[] getSurchargeTypes() {
        return new TaxType[] {
            URBAN_CONSTRUCTION_TAX,
            EDUCATION_SURCHARGE,
            LOCAL_EDUCATION_SURCHARGE
        };
    }
    
    /**
     * 获取进口税类型
     */
    public static TaxType[] getImportTaxTypes() {
        return new TaxType[] {
            CUSTOMS_DUTY,
            IMPORT_VAT,
            IMPORT_CONSUMPTION_TAX
        };
    }
    
    /**
     * 根据代码获取税务类型
     */
    public static TaxType fromCode(String code) {
        for (TaxType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown tax type code: " + code);
    }
    
    /**
     * 获取税率显示字符串
     */
    public String getRateDisplay() {
        return defaultRate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %s @ %s", chineseName, code, description, getRateDisplay());
    }
}