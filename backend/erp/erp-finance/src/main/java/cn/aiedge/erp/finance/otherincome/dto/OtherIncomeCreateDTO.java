package cn.aiedge.erp.finance.otherincome.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 其他收入单保存 DTO（保存草稿/记账共用）
 */
@Data
public class OtherIncomeCreateDTO {

    /** 收入类型: 1-往来单位收入 2-内部收入 */
    private Integer incomeType;

    private Long partnerId;

    private String partnerName;

    private String partnerCode;

    private Long handlerId;

    private String handlerName;

    private Long departmentId;

    private String departmentName;

    private LocalDate incomeDate;

    private String receiptAccount1;

    private BigDecimal receiptAmount1;

    private String receiptAccount2;

    private BigDecimal receiptAmount2;

    private String receiptAccount3;

    private BigDecimal receiptAmount3;

    private String receiptAccount4;

    private BigDecimal receiptAmount4;

    private String accountSubjectCode;

    private String summary;

    private String attachment;

    private String remark;

    private List<OtherIncomeItemDTO> items;
}
