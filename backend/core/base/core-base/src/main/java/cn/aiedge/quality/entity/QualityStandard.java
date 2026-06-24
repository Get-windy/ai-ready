package cn.aiedge.quality.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 质检标准实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_standard")
public class QualityStandard extends BaseEntity {

    /** 标准编码 */
    private String standardCode;

    /** 标准名称 */
    private String standardName;

    /** 检验类型: INBOUND, OUTBOUND, PROCESS */
    private String inspectionType;

    /** 检验项目(JSON数组) */
    private String inspectionItems;

    /** 抽检比例 */
    private BigDecimal sampleRate;

    /** 合格阈值 */
    private BigDecimal passThreshold;

    /** 描述 */
    private String description;

    /** 状态 */
    private Integer status;
}