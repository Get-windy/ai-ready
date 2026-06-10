package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用申请明细VO
 */
@Data
@Schema(description = "费用申请明细")
public class FeeApplicationItemVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "费用项目名称")
    private String itemName;

    @Schema(description = "费用说明")
    private String description;

    @Schema(description = "费用发生日期")
    private LocalDate expenseDate;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "供应商/收款方")
    private String vendorName;

    @Schema(description = "税率(%)")
    private BigDecimal taxRate;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "含税总金额")
    private BigDecimal totalAmountWithTax;

    @Schema(description = "是否有发票")
    private Integer hasInvoice;

    @Schema(description = "发票号码")
    private String invoiceNumber;

    @Schema(description = "会计科目编码")
    private String accountCode;

    @Schema(description = "预算科目编码")
    private String budgetCode;

    @Schema(description = "项目编码")
    private String projectCode;

    @Schema(description = "成本中心")
    private String costCenter;

    @Schema(description = "是否个人垫付")
    private Integer isPersonal;

    @Schema(description = "是否可报销")
    private Integer isReimbursable;

    @Schema(description = "是否已核验")
    private Integer isVerified;

    @Schema(description = "核验人ID")
    private Long verifiedBy;

    @Schema(description = "核验日期")
    private LocalDate verifiedDate;

    @Schema(description = "核验意见")
    private String verificationComment;

    @Schema(description = "序号")
    private Integer sequenceNumber;

    @Schema(description = "备注")
    private String remark;
}
