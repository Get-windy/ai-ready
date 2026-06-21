package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_partner_role")
public class PartnerRole {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long partnerId;
    private String roleType;
    private Integer isPrimary;
    private String status;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
