package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_withdraw_request")
public class WithdrawRequest {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long partnerId;
    private String withdrawNo;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal actualAmount;
    private Long bankAccountId;
    private String accountName;
    private String bankName;
    private String accountNo;
    private String requestReason;
    private String status;
    private Long approveBy;
    private LocalDateTime approveTime;
    private String approveRemark;
    private LocalDateTime payTime;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
