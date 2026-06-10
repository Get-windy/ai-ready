package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.util.Map;

@Data
public class ClientTaskItem {

    private Long taskId;
    private String taskCode;

    /**
     * 渲染好的 HTML 内容（由格式化引擎预处理）
     */
    private String renderedHtml;

    /**
     * 原始模板 JSON（客户端自主渲染备选）
     */
    private Map<String, Object> templateJson;

    /**
     * 单据数据
     */
    private Map<String, Object> dataJson;

    private String printerName;

    private Integer copies;

    private String paperSize;

    private Map<String, Object> paperMargins;
}
