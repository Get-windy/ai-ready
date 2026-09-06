package cn.aiedge.quality.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 缺陷处置完成事件。由 core-base 在缺陷 handle 后发布，
 * 供 core-api 编排服务消费，按处置方式联动下游(采购退货/报损+库存冻结)并回填单号。
 * 依赖方向约束：联动逻辑不能写在 core-base，必须由依赖 core-base 的模块消费。
 */
@Data
@AllArgsConstructor
public class QualityDefectHandledEvent {

    /** 缺陷记录ID */
    private Long defectId;

    /** 处理方式 */
    private String handleType;

    /** 处理数量 */
    private BigDecimal handleQuantity;

    /** 检验记录ID */
    private Long inspectionId;

    /** 来源单号 */
    private String bizNo;

    /** 缺陷等级 */
    private String defectLevel;

    /** 产品ID（来自质检单，供下游定位库存） */
    private Long productId;

    /** 批次号（来自质检单） */
    private String batchNo;

    /** 仓库ID（来自质检单，供报损/退货定位仓库） */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;
}
