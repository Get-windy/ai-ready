package cn.aiedge.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoucherVO {
    
    private Long id;
    
    private String voucherNo;
    
    private Integer voucherType;
    
    private String voucherTypeName;
    
    private String period;
    
    private LocalDate voucherDate;
    
    private Integer wordNo;
    
    private String word;
    
    private BigDecimal totalDebit;
    
    private BigDecimal totalCredit;
    
    private Integer entryCount;
    
    private Integer status;
    
    private String statusName;
    
    private Long preparedBy;
    
    private String preparedByName;
    
    private LocalDateTime preparedTime;
    
    private Long reviewedBy;
    
    private String reviewedByName;
    
    private LocalDateTime reviewedTime;
    
    private Long postedBy;
    
    private String postedByName;
    
    private LocalDateTime postedTime;
    
    private Long cashierId;
    
    private String cashierName;
    
    private String sourceType;
    
    private Long sourceId;
    
    private String sourceNo;
    
    private String remark;
    
    private Integer printed;
    
    private Integer printCount;
    
    private Boolean isBalanced;
    
    private List<VoucherEntryVO> entries;
    
    @Data
    public static class VoucherEntryVO {
        
        private Long id;
        
        private Integer entryNo;
        
        private String summary;
        
        private Long subjectId;
        
        private String subjectCode;
        
        private String subjectName;
        
        private BigDecimal debitAmount;
        
        private BigDecimal creditAmount;
        
        private Integer auxiliaryFlag;
        
        private String auxiliaryValue1;
        
        private String auxiliaryValue2;
        
        private String remark;
    }
}