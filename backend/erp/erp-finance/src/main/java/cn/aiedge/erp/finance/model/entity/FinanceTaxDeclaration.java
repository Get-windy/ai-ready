package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 税务申报记录实体
 * 记录企业税务申报信息
 */
@Data
@TableName("finance_tax_declaration")
@EqualsAndHashCode(callSuper = true)
public class FinanceTaxDeclaration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 申报编号
     */
    @TableField("declaration_no")
    private String declarationNo;

    /**
     * 纳税人识别号
     */
    @TableField("taxpayer_id")
    private String taxpayerId;

    /**
     * 纳税人名称
     */
    @TableField("taxpayer_name")
    private String taxpayerName;

    /**
     * 税种
     * 1-增值税 2-企业所得税 3-个人所得税 4-城市维护建设税 5-教育费附加 6-地方教育附加 7-其他
     */
    @TableField("tax_type")
    private Integer taxType;

    /**
     * 申报期间
     * 格式: YYYY-MM
     */
    @TableField("declaration_period")
    private String declarationPeriod;

    /**
     * 应纳税额
     */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    /**
     * 实际缴税额
     */
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    /**
     * 申报日期
     */
    @TableField("declaration_date")
    private LocalDateTime declarationDate;

    /**
     * 缴税日期
     */
    @TableField("payment_date")
    private LocalDateTime paymentDate;

    /**
     * 申报状态
     * 0-未申报 1-已申报 2-已缴税 3-已更正
     */
    @TableField("status")
    private Integer status = 0;

    /**
     * 申报文件URL
     */
    @TableField("declaration_file")
    private String declarationFile;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
