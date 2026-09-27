package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ChainTaskExecuteRequest {

    @NotNull(message = "chainId 不能为空")
    private Long chainId;

    /**
     * 单据打印数据。
     *
     * <p>可以不给：给了 {@link #pageCode} + {@link #documentId} 时，服务端会走
     * {@code PrintDataProvider} 自己装配（与「单据打印」同一条取数路径）。
     * 两者都没有时按空数据建任务（与改造前一致）。</p>
     */
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
