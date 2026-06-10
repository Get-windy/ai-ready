package cn.aiedge.erp.printing.dto.v2;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class PrintTaskCreateRequest {

    /**
     * 方式一：指定模板直接打印
     */
    private Long templateId;

    /**
     * 方式二：按链路打印
     */
    private Long chainId;

    @NotNull(message = "单据数据不能为空")
    private Map<String, Object> dataJson;

    private String pageCode;

    private String documentType;

    private Long documentId;

    private String documentNo;

    /**
     * 覆盖链路中的客户端（可选）
     */
    private Long clientId;

    /**
     * 覆盖打印机（可选）
     */
    private String printerName;

    private Integer priority;
}
