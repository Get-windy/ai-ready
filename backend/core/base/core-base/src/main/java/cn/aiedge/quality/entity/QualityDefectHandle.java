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

    /** 缺陷类型: QUALITY, PACKAGING, LABELING */
    private String defectType;

    /** 缺陷描述 */
    private String defectDesc;

    /** 处理方式: RETURN, REWORK, SCRAP, SPECIAL_RELEASE */
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

    /** 状态: 0待处理, 1已处理 */
    private Integer status;
}