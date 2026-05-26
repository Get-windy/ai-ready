package cn.aiedge.erp.printing.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrinterDTO {

    private Long id;

    private String printerCode;

    private String printerName;

    private String printerType;

    private String ipAddress;

    private Integer port;

    private String location;

    private String department;

    private Long groupId;

    private String groupName;

    private Integer status;

    private Boolean isDefault;

    private Boolean isOnline;

    private LocalDateTime lastOnlineTime;

    private Integer paperStatus;

    private Integer inkStatus;

    private String remark;
}