package cn.aiedge.dms.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果（《调度任务开发文档》§3.6 工程约束 4：批量操作需二次确认与**结果反馈**）
 *
 * <p>不做「全成功/全失败」的一刀切：逐单给结果，成功数、跳过数、失败原因分别返回，
 * 调度员据此只处理失败的那几条。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "批量操作结果（成功 / 跳过 / 失败逐单反馈）")
public class BatchResultVO {

    @Schema(description = "提交的任务数")
    private int total;

    @Schema(description = "成功数")
    private int success;

    @Schema(description = "失败/跳过数")
    private int failed;

    @Schema(description = "逐单失败原因")
    private List<Item> items = new ArrayList<>();

    @Schema(description = "备注（如：演练模式未落库；无失败时为 null）")
    private String note;

    public void markSuccess() {
        this.success++;
    }

    public void markFailed(Long taskId, String taskNo, String reason) {
        this.failed++;
        Item item = new Item();
        item.setTaskId(taskId);
        item.setTaskNo(taskNo);
        item.setReason(reason);
        this.items.add(item);
    }

    @Data
    @Schema(description = "逐单失败明细")
    public static class Item {

        private Long taskId;

        private String taskNo;

        @Schema(description = "失败/跳过原因")
        private String reason;
    }
}
