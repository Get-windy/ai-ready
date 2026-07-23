package cn.aiedge.wms.borrow.dto;

import cn.aiedge.wms.entity.WmsBorrowReturnItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 借进借出归还请求（支持部分归还，多次调用累计）.
 */
@Data
@Schema(description = "借进借出归还请求")
public class BorrowReturnRequest {

    @NotNull(message = "单据ID不能为空")
    @Schema(description = "借进借出单ID")
    private Long orderId;

    @Schema(description = "归还日期，默认当天")
    private LocalDate returnDate;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "备注")
    private String remark;

    @NotEmpty(message = "归还明细不能为空")
    @Schema(description = "归还明细（orderItemId + quantity）")
    private List<WmsBorrowReturnItem> items;
}
