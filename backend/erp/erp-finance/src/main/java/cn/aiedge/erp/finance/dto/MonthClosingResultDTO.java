package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 月结执行结果DTO（含检查明细）
 */
@Data
public class MonthClosingResultDTO {

    /**
     * 期间编码
     */
    private String periodCode;

    /**
     * 是否全部检查通过（通过则期间已关闭）
     */
    private Boolean success;

    /**
     * 结果说明
     */
    private String message;

    /**
     * 检查项明细
     */
    private List<CheckItem> checks;

    /**
     * 月结操作人
     */
    private String closedBy;

    /**
     * 月结时间
     */
    private LocalDateTime closedTime;

    /**
     * 单项检查结果
     */
    @Data
    public static class CheckItem {
        /**
         * 检查项编码
         */
        private String checkCode;
        /**
         * 检查项名称
         */
        private String checkName;
        /**
         * 是否通过
         */
        private Boolean passed;
        /**
         * 检查明细（如未过账凭证数及凭证号列表）
         */
        private String detail;
    }
}
