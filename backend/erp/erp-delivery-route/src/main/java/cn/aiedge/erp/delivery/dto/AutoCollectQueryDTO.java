package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 围栏自动归集入参
 *
 * 两种调用方式：
 * · 指定 routeId —— 只针对该路线单绑定的围栏做归集；
 * · 不指定 —— 扫描所有「待出发 + 自动归集开启 + 已绑围栏」的路线单，各自归集。
 */
@Data
public class AutoCollectQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 指定路线单（可空） */
    private Long routeId;

    /** 来源范围：BOTH-销售出库单+销售订单（默认） / OUT-仅销售出库单 / SO-仅销售订单 */
    private String source;

    /** 关键字（按单号/客户名过滤，可空） */
    private String keyword;

    /** 单次归集上限（默认 200，防止误扫全量） */
    private Integer limit;

    /** 干跑预览：true 时只返回匹配结果不落库 */
    private Boolean dryRun;
}
