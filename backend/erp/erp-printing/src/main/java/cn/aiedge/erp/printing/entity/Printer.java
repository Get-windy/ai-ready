package cn.aiedge.erp.printing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_printer")
public class Printer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String printerCode;

    private String printerName;

    private String printerType;

    private String ipAddress;

    private Integer port;

    private String location;

    private String department;

    private Long groupId;

    private Integer status;

    private Boolean isDefault;

    private Boolean isOnline;

    private LocalDateTime lastOnlineTime;

    private Integer paperStatus;

    private Integer inkStatus;

    private String remark;

    private Long tenantId;

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