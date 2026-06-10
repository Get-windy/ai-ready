package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 往来对冲实体
 * 同一单位的应收应付对冲操作
 */
@Data
@Accessors(chain = true)
@TableName("erp_offset")
public class Offset {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String offsetNo;

    private String partyType;

    private Long partyId;

    private String partyName;

    private BigDecimal receivableAmount;

    private BigDecimal payableAmount;

    private BigDecimal offsetAmount;

    private BigDecimal balanceAmount;

    private LocalDate offsetDate;

    private String status;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
