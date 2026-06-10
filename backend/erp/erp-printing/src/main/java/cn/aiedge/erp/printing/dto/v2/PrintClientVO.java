package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrintClientVO {

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
    private String machineId;
    private LocalDateTime createdAt;
}
