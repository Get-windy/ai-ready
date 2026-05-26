package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_asset_change")
public class AssetChange {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long assetId;
    
    private String assetCode;
    
    private String assetName;
    
    private Integer changeType;
    
    private LocalDate changeDate;
    
    private BigDecimal beforeValue;
    
    private BigDecimal afterValue;
    
    private BigDecimal changeAmount;
    
    private Integer beforeStatus;
    
    private Integer afterStatus;
    
    private Long beforeDepartmentId;
    
    private String beforeDepartmentName;
    
    private Long afterDepartmentId;
    
    private String afterDepartmentName;
    
    private Long beforeLocationId;
    
    private String beforeLocationName;
    
    private Long afterLocationId;
    
    private String afterLocationName;
    
    private Long beforeCustodianId;
    
    private String beforeCustodianName;
    
    private Long afterCustodianId;
    
    private String afterCustodianName;
    
    private Long voucherId;
    
    private String voucherNo;
    
    private String reason;
    
    private String remark;
    
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
    
    private Long tenantId;
    
    @Version
    private Integer version;
}