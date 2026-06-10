package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChainItemVO {

    private Long itemId;
    private Long chainId;
    private Integer stepOrder;
    private Long templateId;
    private String templateName;
    private String pageCode;
    private Long clientId;
    private String clientName;
    private String printerName;
    private String screenshotMode;
    private Integer screenshotConfirmTimeout;
    private String screenshotConfigJson;
    private LocalDateTime createdAt;
}
