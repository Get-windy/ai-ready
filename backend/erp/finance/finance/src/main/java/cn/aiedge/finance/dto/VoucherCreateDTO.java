package cn.aiedge.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class VoucherCreateDTO {
    
    @NotNull(message = "凭证类型不能为空")
    private Integer voucherType;
    
    private String word;
    
    private String voucherNo;
    
    private String voucherDate;
    
    private String remark;
    
    private String sourceType;
    
    private Long sourceId;
    
    private String sourceNo;
    
    private Long cashierId;
    
    @NotNull(message = "凭证明细不能为空")
    private List<VoucherEntryDTO> entries;
    
    @Data
    public static class VoucherEntryDTO {
        
        private String summary;
        
        @NotNull(message = "科目不能为空")
        private Long subjectId;
        
        private BigDecimal debitAmount;
        
        private BigDecimal creditAmount;
        
        private Long auxiliaryId1;
        
        private Integer auxiliaryType1;
        
        private String auxiliaryValue1;
        
        private Long auxiliaryId2;
        
        private Integer auxiliaryType2;
        
        private String auxiliaryValue2;
        
        private String remark;
    }
}