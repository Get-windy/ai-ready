package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrintChainVO {

    private Long chainId;
    private Long tenantId;
    private String pageCode;
    private String chainName;
    private String description;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ChainItemVO> items;
}
