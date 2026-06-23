package cn.aiedge.erp.sale.saleexchange.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("erp_sale_exchange_approval_record")
public class ExchangeApprovalRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long exchangeId;

    private String action;

    private String actionName;

    private Long operatorId;

    private String operatorName;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
