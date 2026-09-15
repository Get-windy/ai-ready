package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 催单：把指定点位移到目标序号，动态调整其后点位顺序
 *
 * 语义：该点位固定到 targetSeq，其余点位按原相对顺序顺延；
 * replanRest=true 时，对「该点位之后的剩余点位」再按地图能力做最近邻重排
 * （催单点本身不再移动，满足「客户催单必须先到」的业务约束）。
 */
@Data
public class ExpeditePointDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标序号（从 1 开始）；不传按「置顶」处理 */
    private Integer targetSeq;

    /** 目标序号被占用时其余点位是否顺延（默认 true） */
    private Boolean shiftOthers;

    /** 是否对后续剩余点位重新做地图规划（默认 false） */
    private Boolean replanRest;

    /** 催单原因/备注（记录到点位备注，可空） */
    private String reason;
}
