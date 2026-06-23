package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_transaction")
public class PartyTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long partyId;

    private LocalDate transactionDate;

    private Integer transactionType;

    private String documentType;

    private Long documentId;

    private String documentNo;

    private BigDecimal amount;

    private BigDecimal receivableAmount;

    private BigDecimal payableAmount;

    private BigDecimal receivedAmount;

    private BigDecimal paidAmount;

    private BigDecimal balance;

    private String summary;

    private Long operatorId;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
