package cn.aiedge.erp.printing.entity.v2;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_print_client")
public class SysPrintClient {

    @TableId(type = IdType.AUTO)
    private Long clientId;

    private Long tenantId;

    private String clientName;

    private String clientCode;

    private String authKey;

    private String status;

    private LocalDateTime lastHeartbeat;

    private String clientIp;

    private String clientVersion;

    private String defaultPrinter;

    /** 客户端机器标识（首次启动生成，用于识别同一台机器） */
    private String machineId;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
