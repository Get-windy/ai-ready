package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 围栏自动归集结果（含逐条原因，便于运营判断「为什么没进来」） */
@Data
public class AutoCollectResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 扫描到的配送需求总数 */
    private int scanned;

    /** 命中围栏的需求数 */
    private int matched;

    /** 实际入线（或预览将入线）的需求数 */
    private int added;

    /** 已在其它路线单中（未重复入线） */
    private int alreadyOnRoute;

    /** 客户未维护配送坐标，无法判定 */
    private int missingCoordinate;

    /** 落在围栏外 */
    private int outsideFence;

    /** 干跑预览（true 时不落库） */
    private boolean dryRun;

    /** 逐条明细（最多返回 200 条） */
    private List<AutoCollectResultVO.Item> items = new ArrayList<>();

    /** 说明信息（如降级、上限截断） */
    private List<String> messages = new ArrayList<>();

    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 目标路线单ID（预览未落库时为匹配到的路线单） */
        private Long routeId;

        private String routeCode;

        private String fenceName;

        private String sourceType;

        private Long sourceId;

        private String billNo;

        private String customerName;

        private String address;

        /** ADDED-已入线 / PREVIEW-预览命中 / ALREADY_ON_ROUTE-已在线上 / NO_COORD-缺坐标 / OUTSIDE-围栏外 */
        private String result;

        /** 结果说明 */
        private String reason;
    }
}
