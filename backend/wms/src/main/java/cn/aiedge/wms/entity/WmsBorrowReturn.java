package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_borrow_return")
@Schema(description = "借进借出归还记录")
public class WmsBorrowReturn extends BaseEntity {
    @Schema(description = "关联借进借出单ID")
    private Long orderId;
    @Schema(description = "归还日期")
    private LocalDate returnDate;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "操作人姓名")
    private String operatorName;
    @Schema(description = "备注")
    private String remark;
}
