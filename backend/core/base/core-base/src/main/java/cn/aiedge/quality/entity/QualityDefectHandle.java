package cn.aiedge.quality.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 不合格处理实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_defect_handle")
public class QualityDefectHandle extends BaseEntity {

    /** 检验记录ID */
    private Long inspectionId;

    /** 来源单号(质检单的业务单号) */
    private String bizNo;

    /** 缺陷类型: QUALITY, PACKAGING, LABELING */
    private String defectType;

    /** 缺陷描述 */
    private String defectDesc;

    /** 缺陷等级: S严重, Ma主要, Mi次要 */
    private String defectLevel;

    /** 纠正措施(CAPA) */
    private String correctiveAction;

    /** 预防措施(CAPA) */
    private String preventiveAction;

    /** 缺陷数量 */
    private BigDecimal defectQuantity;

    /** 处理方式: RETURN退货, CONCESSION让步接收, REWORK返工, SCRAP报废, SPECIAL_RELEASE特采 */
    private String handleType;

    /** 处理数量 */
    private BigDecimal handleQuantity;

    /** 处理人ID */
    private Long handlerId;

    /** 处理人姓名 */
    private String handlerName;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理结果 */
    private String handleResult;

    /** 处置生成的采购退货单号(RETURN) */
    private String returnNo;

    /** 处置生成的报损单号(SCRAP) */
    private String damageNo;

    /** 状态: 0待处理, 1已处理 */
    private Integer status;
}