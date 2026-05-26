package cn.aiedge.finance.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发票DTO
 */
@Data
public class InvoiceDTO {
    
    private Long id;
    
    @NotBlank(message = "发票号码不能为空")
    @Pattern(regexp = "^\\d{20}$", message = "发票号码必须为20位数字")
    private String invoiceNo;
    
    @NotNull(message = "发票类型不能为空")
    @Min(value = 1, message = "发票类型错误")
    @Max(value = 5, message = "发票类型错误")
    private Integer invoiceType;
    
    @NotNull(message = "发票方向不能为空")
    @Min(value = 1, message = "发票方向错误")
    @Max(value = 2, message = "发票方向错误")
    private Integer direction;
    
    @NotNull(message = "开票日期不能为空")
    private LocalDate invoiceDate;
    
    private Integer bizType;
    private Long bizId;
    private String bizNo;
    
    @NotBlank(message = "销方名称不能为空")
    private String sellerName;
    
    @Pattern(regexp = "^[A-Z0-9]{15,20}$", message = "税号格式错误")
    private String sellerTaxNo;
    
    private String sellerAddressPhone;
    private String sellerBankAccount;
    
    @NotBlank(message = "购方名称不能为空")
    private String buyerName;
    
    @Pattern(regexp = "^[A-Z0-9]{15,20}$", message = "税号格式错误")
    private String buyerTaxNo;
    
    private String buyerAddressPhone;
    private String buyerBankAccount;
    
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00", message = "金额不能为负数")
    private BigDecimal amount;
    
    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    private BigDecimal taxRate;
    
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    
    private String currency;
    private String remark;
    private String drawer;
    private String reviewer;
    private String payee;
    
    @Valid
    @NotEmpty(message = "发票明细不能为空")
    private List<InvoiceItemDTO> items;
}
