package cn.aiedge.quality.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 缺陷处理历史（支撑多步处置/处理历史闭环）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_defect_handle_history")
public class QualityDefectHandleHistory extends BaseEntity {

    /** 缺陷记录ID */
    private Long defectId;

    /** 处理方式: RETURN退货, CONCESSION让步接收, REWORK返工, SCRAP报废, SPECIAL_RELEASE特采 */
    private String handleType;

    /** 处理数量 */
    private BigDecimal handleQuantity;

    /** 处理结果 */
    private String handleResult;

    /** 处理人ID */
    private Long handlerId;

    /** 处理人姓名 */
    private String handlerName;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 该步处置生成的采购退货单号(RETURN) */
    private String returnNo;

    /** 该步处置生成的报损单号(SCRAP) */
    private String damageNo;

    /** 该步处置的纠正措施(CAPA) */
    private String correctiveAction;

    /** 该步处置的预防措施(CAPA) */
    private String preventiveAction;
}
