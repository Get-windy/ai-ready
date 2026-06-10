package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ChainTaskExecuteRequest {

    @NotNull(message = "chainId 不能为空")
    private Long chainId;

    @NotNull(message = "单据数据不能为空")
    private Map<String, Object> dataJson;

    private String pageCode;

    private String documentType;

    private Long documentId;

    private String documentNo;

    /**
     * 起始步骤（从 1 开始），默认 1
     */
    private Integer startStep;

    /**
     * 结束步骤，默认直到最后一步
     */
    private Integer endStep;

    private Integer priority;
}
