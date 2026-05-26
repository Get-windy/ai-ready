package cn.aiedge.erp.finance.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 税务申报记录实体
 * 记录企业税务申报信息
 */
@Data
@Entity
@Table(name = "finance_tax_declaration")
@EqualsAndHashCode(callSuper = true)
public class FinanceTaxDeclaration extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 申报编号
     */
    @Column(name = "declaration_no", nullable = false, unique = true, length = 32)
    private String declarationNo;
    
    /**
     * 纳税人识别号
     */
    @Column(name = "taxpayer_id", length = 20)
    private String taxpayerId;
    
    /**
     * 纳税人名称
     */
    @Column(name = "taxpayer_name", length = 100)
    private String taxpayerName;
    
    /**
     * 税种
     * 1-增值税 2-企业所得税 3-个人所得税 4-城市维护建设税 5-教育费附加 6-地方教育附加 7-其他
     */
    @Column(name = "tax_type", nullable = false)
    private Integer taxType;
    
    /**
     * 申报期间
     * 格式: YYYY-MM
     */
    @Column(name = "declaration_period", nullable = false, length = 7)
    private String declarationPeriod;
    
    /**
     * 应纳税额
     */
    @Column(name = "tax_amount", precision = 18, scale = 2)
    private Double taxAmount;
    
    /**
     * 实际缴税额
     */
    @Column(name = "paid_amount", precision = 18, scale = 2)
    private Double paidAmount;
    
    /**
     * 申报日期
     */
    @Column(name = "declaration_date")
    private LocalDateTime declarationDate;
    
    /**
     * 缴税日期
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    /**
     * 申报状态
     * 0-未申报 1-已申报 2-已缴税 3-已更正
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;
    
    /**
     * 申报文件URL
     */
    @Column(name = "declaration_file", length = 500)
    private String declarationFile;
    
    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;
}
