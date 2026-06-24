package cn.aiedge.quality.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检验记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_inspection")
public class QualityInspection extends BaseEntity {

    /** 业务ID */
    private Long bizId;

    /** 业务类型: PURCHASE_ORDER, SALE_ORDER, STOCK_IN, STOCK_OUT */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** 产品ID */
    private Long productId;

    /** 产品名称 */
    private String productName;

    /** 批次号 */
    private String batchNo;

    /** 检验数量 */
    private BigDecimal quantity;

    /** 抽检数量 */
    private BigDecimal sampleQuantity;

    /** 检验结果: PASS, FAIL, PENDING */
    private String inspectionResult;

    /** 合格数量 */
    private BigDecimal passQuantity;

    /** 不合格数量 */
    private BigDecimal failQuantity;

    /** 检验项目结果(JSON) */
    private String inspectionItems;

    /** 检验员ID */
    private Long inspectorId;

    /** 检验员姓名 */
    private String inspectorName;

    /** 检验时间 */
    private LocalDateTime inspectionTime;

    /** 备注 */
    private String remark;
}