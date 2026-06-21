package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_partner_logistics_ext")
public class PartnerLogisticsExt {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long partnerId;
    private Integer logisticsType;
    private String serviceArea;
    private String transportModes;
    private Integer vehicleCount;
    private Integer coldChain;
    private Integer hazardous;
    private String servicePhone;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
